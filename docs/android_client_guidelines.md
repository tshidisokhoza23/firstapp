# Android Client Guidelines for Sync, Dedup, and Privacy

## 1) Message Send/Dedup Rules

- Generate UUID on device before sending each message.
- Include it as client_uuid in API payload.
- Optimistic UI:
  - Add local item with status sending.
- On WebSocket receive:
  - If same client_uuid exists locally, update status to sent and merge server metadata.
  - Else append as new message.

Suggested data model fields:
- id: String (server UUID)
- clientUuid: String
- conversationId: String
- senderId: String
- messageText: String
- mediaType: String
- createdAt: Long
- status: sending | sent | failed

## 2) Cursor-based History Fetch

Use:
- GET /chats/{conversationID}/messages?limit=50&cursor={timestamp}

Client pagination behavior:
- First load without cursor for latest page.
- For older page, set cursor to oldest createdAt from currently loaded list.

## 3) Display Name Hierarchy

Keep this strict order for UI labels:
1. Local phone contacts name lookup
2. Remote full_name/profile display name
3. Raw phone number

Existing utility:
- app/src/main/java/com/example/firstapp/common/util/DisplayNameManager.kt

## 4) Closed Application Chat UX

- If API returns 403 with closed conversation message:
  - Disable text input for applicant.
  - Show closed notice banner.
- Keep historical messages readable.

## 5) Phone Number Privacy

- Never render raw phone number in public jobs or public chat cards.
- Use user ID + display name + avatar/card metadata.

## 6) Media and Shorts Ad Rendering Contracts

Media feed:
- Item types expected:
  - POST
  - INTERNAL_AD
  - EXTERNAL_AD_PLACEHOLDER

Replies stream:
- Item types expected:
  - POST
  - EXTERNAL_AD_PLACEHOLDER

Shorts feed:
- Item types expected:
  - ORGANIC_SHORT
  - INTERNAL_VIDEO_AD
  - EXTERNAL_VIDEO_AD_PLACEHOLDER

Renderer guidance:
- If placeholder type, mount AdMob view container for corresponding placement.
- If internal ad type, render as normal post/short card with sponsored badge.

## 7) CV Application Card Handling

When media_type is JOB_POST and applicant_profile is present:
- Render inline application card with:
  - full_name
  - bio
  - skills chips
  - CV view/download action using cv_url

## 8) Connection and Sync Resilience

- On reconnect:
  - resubscribe websocket.
  - fetch latest cursor page to patch missed events.
- Keep local Room cache as source of scroll state, remote as source of truth.
