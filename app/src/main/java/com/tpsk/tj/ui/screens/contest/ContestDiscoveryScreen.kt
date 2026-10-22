package com.tpsk.tj.ui.screens.contest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.media.data.api.ApiContest

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
        val sort = if (selectedFilter == "New") "new" else "trending"
        viewModel.loadContests(searchQuery, sort)
    }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

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
                placeholder = { Text("Search live contests", color = secondaryTextColor, fontSize = 14.sp) },
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
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = surfaceColor,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Trending", "New")) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                listState.isLoading && listState.contests.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                listState.error != null && listState.contests.isEmpty() -> {
                    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(listState.error ?: "Could not load contests", color = secondaryTextColor)
                        TextButton(onClick = onCreateContestClick) { Text("Host a contest") }
                    }
                }
                listState.contests.isEmpty() -> {
                    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No live contests yet", color = textColor, fontWeight = FontWeight.Bold)
                        Text("Host one and share manager codes with contestants.", color = secondaryTextColor, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onCreateContestClick) { Text("Host a contest") }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(listState.contests, key = { it.id.ifBlank { it.hashtag } }) { contest ->
                            ContestRow(contest, surfaceColor, textColor, secondaryTextColor, accentColor) {
                                onSelectContest(contest.hashtag.ifBlank { contest.title })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContestRow(
    contest: ApiContest,
    surfaceColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text("#${contest.hashtag.removePrefix("#")}", color = accentColor, fontWeight = FontWeight.Bold)
        Text(contest.title, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        if (contest.description.isNotBlank()) {
            Text(contest.description, color = secondaryTextColor, fontSize = 13.sp, maxLines = 2)
        }
        Text(
            "${contest.voteCount} votes · ${contest.entryCount} entries",
            color = GoldYellow,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
