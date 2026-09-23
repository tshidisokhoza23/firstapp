# Mingle Module: Complete Technical Specification & Implementation Guide

## 1. System Overview & Clean UI Retention

The Mingle module provides location-aware candidate discovery, interaction logging, and real-time private messaging.

- All current screen layouts, typography, paddings, and header icons (Inbox, Activity Bell, Refresh, Radius Chip, Add Photo) remain strictly intact.
- Messaging logic integrates directly into the unified conversations architecture (`conversation_type = 'mingle_secret'`) rather than isolated chat schemas.

## 2. UI / UX Polish & Corrections

- **Gender Chip Text Wrapping:** Set `softWrap = false` or provide `PaddingValues(horizontal = 14.dp)` on profile chips to prevent the "Other" tag from wrapping awkwardly into "Oth / er".
- **Header Text Truncation:** Complete the truncated subtitle on the Secret Chats screen from "Private secret chats with your" to "Private secret chats with your mutual matches" with appropriate ellipsis handling.
- **Slider Track & Thumb Balance:** Replace sharp vertical dividers with a standard Material 3 styled slider thumb to ensure clean visual weight over the pill track.
- **Ad Inset & Gesture Collision:** Wrap bottom banner ad placements in `Modifier.navigationBarsPadding()` so live ad banners do not collide with Android system gesture insets.
- **Scroll Padding:** Apply `PaddingValues(bottom = adHeight + navBarsPadding)` to lazy containers so the last candidate or conversation item is never obscured by banner ads.

## 3. Database Schema & Indexing (PostgreSQL + PostGIS)

```sql
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Profiles & Geolocation
CREATE TABLE IF NOT EXISTS mingle_profiles (
    user_id UUID PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    looking_for VARCHAR(20) NOT NULL,
    bio TEXT,
    photos TEXT[] DEFAULT '{}',
    phone_hash VARCHAR(64) NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mingle_profiles_location ON mingle_profiles USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_mingle_profiles_phone ON mingle_profiles (phone_hash);

-- 2. Directional Interactions
CREATE TABLE IF NOT EXISTS mingle_swipes (
    swiper_id UUID NOT NULL,
    target_id UUID NOT NULL,
    action VARCHAR(10) NOT NULL CHECK (action IN ('like', 'pass')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (swiper_id, target_id)
);

-- 3. Contact Privacy Filtering
CREATE TABLE IF NOT EXISTS user_blocked_contacts (
    user_id UUID NOT NULL,
    contact_phone_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, contact_phone_hash)
);

CREATE INDEX IF NOT EXISTS idx_user_blocked_contacts_hash ON user_blocked_contacts (contact_phone_hash);

-- 4. Activity & Notifications (Bell Screen)
CREATE TABLE IF NOT EXISTS mingle_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    event_type VARCHAR(20) NOT NULL CHECK (event_type IN ('like_received', 'mutual_match')),
    conversation_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Safety, Blocks & Moderation Reports
CREATE TABLE IF NOT EXISTS mingle_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    reported_id UUID NOT NULL,
    reason VARCHAR(50) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## 4. Hardened Database Migration Script (`000048_mingle_hardening.up.sql`)

```sql
-- 1. Deterministic conversation pair key to prevent duplicate match conversations
ALTER TABLE conversations 
ADD COLUMN IF NOT EXISTS pair_key TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS idx_conversations_mingle_secret_pair
ON conversations (pair_key)
WHERE LOWER(type) = 'mingle' AND deleted_at IS NULL;

-- 2. Performance index on blocked contacts / user_blocks for fast candidate loading
CREATE INDEX IF NOT EXISTS idx_user_blocks_lookup 
ON user_blocks (blocker_id, blocked_id);

-- 3. Moderation workflow and audit fields
ALTER TABLE mingle.profiles 
ADD COLUMN IF NOT EXISTS deleted_at BIGINT DEFAULT NULL;

-- 4. Activity deduplication: Prevent spamming likes
CREATE UNIQUE INDEX IF NOT EXISTS idx_mingle_activities_recent_like
ON mingle.activities (user_id, actor_id, type)
WHERE type = 'like';
```

## 5. Backend Engine (Go)

### 5.1 Data Models & Structs

```go
package mingle

import (
	"context"
	"database/sql"
	"errors"
	"strings"
	"time"

	"github.com/lib/pq"
)

type DiscoveryRequest struct {
	UserID    string  `json:"user_id"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
	RadiusKm  int     `json:"radius_km"`
	Limit     int     `json:"limit"`
}

type CandidateProfile struct {
	UserID      string   `json:"user_id"`
	DisplayName string   `json:"display_name"`
	Age         int      `json:"age"`
	Gender      string   `json:"gender"`
	Bio         string   `json:"bio"`
	Photos      []string `json:"photos"`
	PhoneHash   string   `json:"-"`
	DistanceKm  float64  `json:"distance_km"`
}

type DiscoveryResponse struct {
	Candidates   []CandidateProfile `json:"candidates"`
	IsExpanded   bool               `json:"is_expanded"`
	ActiveRadius int                `json:"active_radius_km"`
}

type SwipeResult struct {
	IsMatch        bool   `json:"is_match"`
	ConversationID string `json:"conversation_id,omitempty"`
}

func makePairKey(a, b string) string {
	if a < b {
		return a + ":" + b
	}
	return b + ":" + a
}
```

### 5.2 Hardened Discovery & Swiping Engine

```go
func (s *MingleService) GetDiscoveryCandidates(ctx context.Context, req DiscoveryRequest) (*DiscoveryResponse, error) {
	if req.Limit <= 0 {
		req.Limit = 20
	}
	fetchLimit := req.Limit * 2

	var myPhoneHash string
	err := s.db.QueryRowContext(ctx, `SELECT phone_hash FROM mingle_profiles WHERE user_id = $1`, req.UserID).Scan(&myPhoneHash)
	if err != nil {
		return nil, err
	}

	blockedRows, err := s.db.QueryContext(ctx, `SELECT contact_phone_hash FROM user_blocked_contacts WHERE user_id = $1`, req.UserID)
	if err != nil {
		return nil, err
	}
	defer blockedRows.Close()

	blockedSet := make(map[string]struct{})
	for blockedRows.Next() {
		var h string
		if err := blockedRows.Scan(&h); err == nil {
			blockedSet[h] = struct{}{}
		}
	}

	radiusMeters := req.RadiusKm * 1000
	primaryQuery := `
		SELECT 
			p.user_id, p.display_name, p.age, p.gender, COALESCE(p.bio, ''), p.photos, p.phone_hash,
			ROUND((ST_Distance(p.location, ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography) / 1000)::numeric, 1) AS distance_km
		FROM mingle_profiles p
		LEFT JOIN mingle_swipes s ON s.swiper_id = $3 AND s.target_id = p.user_id
		WHERE p.user_id != $3
		  AND p.is_active = TRUE
		  AND p.deleted_at IS NULL
		  AND cardinality(p.photos) > 0
		  AND s.target_id IS NULL
		  AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography, $4)
		ORDER BY ST_Distance(p.location, ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography) ASC
		LIMIT $5;
	`

	rawCandidates, err := s.queryCandidates(ctx, primaryQuery, req.Longitude, req.Latitude, req.UserID, radiusMeters, fetchLimit)
	if err != nil {
		return nil, err
	}

	candidates := s.filterBlockedCandidates(ctx, rawCandidates, blockedSet, myPhoneHash, req.Limit)
	if len(candidates) > 0 {
		return &DiscoveryResponse{
			Candidates:   candidates,
			IsExpanded:   false,
			ActiveRadius: req.RadiusKm,
		}, nil
	}

	fallbackQuery := `
		SELECT 
			p.user_id, p.display_name, p.age, p.gender, COALESCE(p.bio, ''), p.photos, p.phone_hash,
			ROUND((ST_Distance(p.location, ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography) / 1000)::numeric, 1) AS distance_km
		FROM mingle_profiles p
		LEFT JOIN mingle_swipes s ON s.swiper_id = $3 AND s.target_id = p.user_id
		WHERE p.user_id != $3
		  AND p.is_active = TRUE
		  AND p.deleted_at IS NULL
		  AND cardinality(p.photos) > 0
		  AND s.target_id IS NULL
		  AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography, 500000) -- 500 km soft bound
		ORDER BY p.location <-> ST_SetSRID(ST_MakePoint($1, $2), 4326)::geography ASC
		LIMIT $4;
	`

	rawFallback, err := s.queryCandidates(ctx, fallbackQuery, req.Longitude, req.Latitude, req.UserID, fetchLimit)
	if err != nil {
		return nil, err
	}

	fallbackFiltered := s.filterBlockedCandidates(ctx, rawFallback, blockedSet, myPhoneHash, req.Limit)
	return &DiscoveryResponse{
		Candidates:   fallbackFiltered,
		IsExpanded:   true,
		ActiveRadius: req.RadiusKm,
	}, nil
}
```

### 5.3 Race-Proof `RecordSwipe`

```go
func (s *MingleService) RecordSwipe(ctx context.Context, swiperID, targetID, action string) (*SwipeResult, error) {
	tx, err := s.db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelReadCommitted})
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	// 1. Record Swipe (Upsert)
	_, err = tx.ExecContext(ctx, `
		INSERT INTO mingle_swipes (swiper_id, target_id, action, updated_at)
		VALUES ($1, $2, $3, NOW())
		ON CONFLICT (swiper_id, target_id) DO UPDATE SET action = $3, updated_at = NOW()
	`, swiperID, targetID, action)
	if err != nil {
		return nil, err
	}

	if action != "like" {
		return &SwipeResult{IsMatch: false}, tx.Commit()
	}

	// 2. Check reciprocal like
	var mutual bool
	err = tx.QueryRowContext(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM mingle_swipes 
			WHERE swiper_id = $1 AND target_id = $2 AND action = 'like'
		)
	`, targetID, swiperID).Scan(&mutual)
	if err != nil {
		return nil, err
	}

	if !mutual {
		_, err = tx.ExecContext(ctx, `
			INSERT INTO mingle_activity (user_id, actor_id, event_type, created_at)
			VALUES ($1, $2, 'like_received', NOW())
			ON CONFLICT (user_id, actor_id, event_type) 
			WHERE event_type = 'like_received' DO NOTHING
		`, targetID, swiperID)
		if err != nil {
			return nil, err
		}

		return &SwipeResult{IsMatch: false}, tx.Commit()
	}

	// 3. Mutual Match: Resolve conversation via deterministic pair key
	pairKey := makePairKey(swiperID, targetID)
	var conversationID string

	err = tx.QueryRowContext(ctx, `
		INSERT INTO conversations (conversation_type, pair_key, is_active, created_at, updated_at)
		VALUES ('mingle_secret', $1, TRUE, NOW(), NOW())
		ON CONFLICT (pair_key) WHERE conversation_type = 'mingle_secret' AND is_active = TRUE
		DO UPDATE SET updated_at = NOW()
		RETURNING id
	`, pairKey).Scan(&conversationID)
	if err != nil {
		return nil, err
	}

	_, err = tx.ExecContext(ctx, `
		INSERT INTO conversation_participants (conversation_id, user_id)
		VALUES ($1, $2), ($1, $3)
		ON CONFLICT DO NOTHING
	`, conversationID, swiperID, targetID)
	if err != nil {
		return nil, err
	}

	_, err = tx.ExecContext(ctx, `
		INSERT INTO mingle_activity (user_id, actor_id, event_type, conversation_id, created_at)
		VALUES 
			($1, $2, 'mutual_match', $3, NOW()),
			($2, $1, 'mutual_match', $3, NOW())
	`, swiperID, targetID, conversationID)
	if err != nil {
		return nil, err
	}

	if err := tx.Commit(); err != nil {
		return nil, err
	}

	go s.notifier.SendPush(targetID, "New Match!", "You have a new mutual match in Mingle Secret Chats!")
	go s.notifier.SendPush(swiperID, "New Match!", "It's a match! Say hello in Secret Chats.")

	return &SwipeResult{IsMatch: true, ConversationID: conversationID}, nil
}
```

## 6. Client Integration (Jetpack Compose)

### 6.1 Discovery Radius Bottom Sheet

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryRadiusBottomSheet(
    currentRadius: Int,
    onDismissRequest: () -> Unit,
    onApplyRadius: (Int) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentRadius.toFloat()) }
    val displayValue = sliderPosition.roundToInt()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Discovery Radius",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (displayValue >= 100) {
                    "Showing people everywhere (expanding beyond 100 km)"
                } else {
                    "Show people within $displayValue km of your location"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(28.dp))
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF0F3995),
                    activeTrackColor = Color(0xFF0F3995),
                    inactiveTrackColor = Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 km", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(
                    text = if (displayValue >= 100) "100+ km" else "$displayValue km",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F3995)
                )
                Text("100 km", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onApplyRadius(displayValue) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3995))
            ) {
                Text("Apply Filter", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

## 7. Play Store Compliance & Lifecycle Safeguards

- **Profile Gatekeeper:** Users cannot enter Discovery or be returned in queries without at least 1 verified profile picture (`cardinality(photos) > 0`).
- **Age Verification:** Date of birth enforcement ensures all participants are 18+ prior to discovery activation.
- **Mandatory In-Chat Moderation:** All conversations initiated from Secret Chats feature a dedicated three-dot menu supporting Block User and Report User (Spam, Inappropriate Media, Harassment).
- **Bi-directional Teardown:** Blocking immediately sets `conversations.is_active = FALSE`, clears mutual records, and bidirectionally excludes both accounts from future discovery queries.
- **Location Battery Optimization:** Coordinate checks execute only upon opening the Mingle tab, updating the backend only if the device has moved $\ge 500\text{ meters}$.
- **Screen Privacy:** Secret Chat views enforce `WindowManager.LayoutParams.FLAG_SECURE` to prevent unauthorized screenshots or screen capture.
- **40-Point Balance Maintenance Rule:** Users must maintain at least 40 points in their wallet to access Mingle. Before liking a candidate, if a user has less than 50 points ($40 + 10\text{ like cost}$), the Points Gate / Rewarded Ad dialog opens to prompt them to earn points, preventing their balance from dropping to 30 points.
