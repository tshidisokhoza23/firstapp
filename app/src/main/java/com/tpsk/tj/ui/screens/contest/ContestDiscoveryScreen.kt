package com.tpsk.tj.ui.screens.contest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.media.data.api.ApiContest
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

val GoldYellow = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestDiscoveryScreen(
    onNavigateBack: () -> Unit,
    onSelectContest: (String) -> Unit,
    onCreateContestClick: () -> Unit,
    viewModel: ContestViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Trending") }
    val listState by viewModel.listState.collectAsState()

    LaunchedEffect(searchQuery, selectedFilter) {
        if (searchQuery.isNotEmpty()) delay(250)
        val sort = when (selectedFilter) {
            "New" -> "new"
            "Ending Soon" -> "ending"
            else -> "trending"
        }
        viewModel.loadContests(searchQuery, sort)
    }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary
    val contests = listState.contests

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contests & Hashtags", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    TextButton(onClick = onCreateContestClick) {
                        Text("+ Host", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                placeholder = { Text("Search contests", color = secondaryTextColor, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = secondaryTextColor) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = secondaryTextColor)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            if (searchQuery.isNotEmpty()) {
                Text("Search Results", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                when {
                    listState.isLoading && contests.isEmpty() -> {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }
                    contests.isEmpty() -> {
                        Text("No contests match that search.", color = secondaryTextColor, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    else -> {
                        LazyColumn {
                            items(contests, key = { it.id.ifBlank { it.hashtag } }) { contest ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectContest(displayTag(contest)) }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(displayTag(contest), color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                        Text("Active • ${formatVoters(contest.voteCount)}", color = secondaryTextColor, fontSize = 12.sp)
                                    }
                                    Text(endsInLabel(contest.endsAt).ifBlank { "Live" }, color = secondaryTextColor, fontSize = 12.sp)
                                }
                                HorizontalDivider(color = surfaceColor)
                            }
                        }
                    }
                }
            } else {
                Text("🔥 Top Contests This Week", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                if (listState.isLoading && contests.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else if (contests.isEmpty()) {
                    Text("No live contests yet. Host one to start the board.", color = secondaryTextColor, fontSize = 13.sp)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(contests.take(8), key = { _, contest -> contest.id.ifBlank { contest.hashtag } }) { idx, contest ->
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { onSelectContest(displayTag(contest)) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = surfaceColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        ContestFace(
                                            url = contest.previewAvatarUrl ?: contest.coverUrl,
                                            label = contest.previewName ?: displayTag(contest),
                                            modifier = Modifier.size(64.dp).clip(CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier.size(20.dp).background(GoldYellow, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${idx + 1}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        displayTag(contest),
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(formatVoters(contest.voteCount), color = secondaryTextColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Trending", "Ending Soon", "New").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White,
                                containerColor = surfaceColor,
                                labelColor = secondaryTextColor
                            ),
                            border = null,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Text("Active Contests", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                if (listState.error != null && contests.isEmpty()) {
                    Text(listState.error ?: "Could not load contests", color = secondaryTextColor)
                    TextButton(onClick = onCreateContestClick) { Text("Host a contest") }
                } else if (contests.isEmpty() && !listState.isLoading) {
                    Text("Nothing is live in this filter.", color = secondaryTextColor, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onCreateContestClick) { Text("Host a contest") }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(contests, key = { it.id.ifBlank { it.hashtag } }) { contest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceColor, RoundedCornerShape(12.dp))
                                    .clickable { onSelectContest(displayTag(contest)) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ContestFace(
                                    url = contest.coverUrl ?: contest.previewAvatarUrl,
                                    label = contest.previewName ?: displayTag(contest),
                                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(displayTag(contest), color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${contest.entryCount} Contestants • ${endsInLabel(contest.endsAt).ifBlank { "Live" }}",
                                        color = secondaryTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContestFace(url: String?, label: String, modifier: Modifier) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = label,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Text(label.removePrefix("#").take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun displayTag(contest: ApiContest): String {
    val raw = contest.hashtag.ifBlank { contest.title }
    return if (raw.startsWith("#")) raw else "#$raw"
}

internal fun formatVoters(count: Int): String {
    if (count >= 1000) {
        val thousands = count / 1000.0
        val label = if (count % 1000 == 0) "${count / 1000}k" else String.format("%.1fk", thousands)
        return "$label voters"
    }
    return "$count voters"
}

internal fun formatVotes(count: Int): String = "%,d".format(count)

internal fun endsInLabel(raw: String?): String {
    val instant = parseContestInstant(raw) ?: return ""
    val diff = Duration.between(Instant.now(), instant)
    if (diff.isNegative || diff.isZero) return "Ended"
    val days = diff.toDays()
    val hours = diff.toHours() % 24
    return if (days > 0) "Ends in ${days}d ${hours}h" else "Ends in ${diff.toHours().coerceAtLeast(1)}h"
}

internal fun contestWeekLabel(createdAt: String?): String {
    val instant = parseContestInstant(createdAt) ?: return "Week 1"
    val days = Duration.between(instant, Instant.now()).toDays().coerceAtLeast(0)
    return "Week ${days / 7 + 1}"
}

private fun parseContestInstant(raw: String?): Instant? {
    if (raw.isNullOrBlank()) return null
    return runCatching { Instant.parse(raw) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(raw).toInstant() }.getOrNull()
}
