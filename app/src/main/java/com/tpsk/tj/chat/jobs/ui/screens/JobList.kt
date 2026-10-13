@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.ui.components.JobItem
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.home.ui.components.AppLogo
import com.tpsk.tj.ui.navigation.Screen

@Composable
fun JobsHomeScreen(
    navController: NavController,
    bottomPadding: Dp = 0.dp,
    viewModel: JobViewModel = hiltViewModel()
) {
    val jobsState by viewModel.jobs.collectAsState()
    val feedChip by viewModel.feedChip.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filters = listOf(
        "All", "General", "IT", "Finance", "Sales", "Healthcare", "Education",
        "Engineering", "Marketing", "Hospitality", "Logistics", "Admin", "Legal", "Construction"
    )

    val backgroundBrush = if (com.tpsk.tj.common.ui.theme.LocalIsDarkTheme.current) {
        Brush.verticalGradient(
            colors = listOf(
                com.tpsk.tj.common.ui.theme.AppBackgroundDarkTop,
                com.tpsk.tj.common.ui.theme.AppBackgroundDarkBottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.White, com.tpsk.tj.common.ui.theme.AppBackgroundLight)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush).padding(bottom = bottomPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar with App Logo matching Social tab style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .statusBarsPadding()
            ) {
                if (isSearchActive) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        color = Color.Transparent
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    viewModel.search(it)
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search jobs...", color = Color.Gray) },
                                leadingIcon = {
                                    IconButton(onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                        viewModel.search("")
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            searchQuery = ""
                                            viewModel.search("")
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                } else {
                    CenterAlignedTopAppBar(
                        title = { AppLogo(size = 36.dp) },
                        navigationIcon = {
                            IconButton(onClick = {}) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }

            // Job Category Chips Row ALWAYS visible at the top
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    val isSelected = (selectedFilter == filter) || (feedChip == filter)
                    com.tpsk.tj.common.ui.components.AppFilterChip(
                        text = filter,
                        isSelected = isSelected,
                        onClick = {
                            selectedFilter = filter
                            viewModel.selectFeedChip(filter)
                        }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = jobsState) {
                    is Resource.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message ?: "Could not load jobs",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.fetchJobs() }) { Text("Retry") }
                        }
                    }
                    is Resource.Loading -> {
                        if (state.data.isNullOrEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            JobList(
                                jobs = filterJobs(state.data ?: emptyList(), selectedFilter),
                                onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) }
                            )
                        }
                    }
                    is Resource.Success -> {
                        JobList(
                            jobs = filterJobs(state.data ?: emptyList(), selectedFilter),
                            onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) }
                        )
                    }
                }
            }
        }

        // Job Feed Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { navController.navigate(Screen.PostJob.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Post a Job",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun filterJobs(jobs: List<Job>, filter: String): List<Job> {
    if (filter == "All") return jobs
    return jobs.filter { job ->
        job.jobType.contains(filter, ignoreCase = true) ||
        job.title.contains(filter, ignoreCase = true) ||
        job.description.contains(filter, ignoreCase = true)
    }
}

@Composable
fun JobList(jobs: List<Job>, topBarHeight: Dp = 0.dp, onJobClick: (Job) -> Unit) {
    if (jobs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No jobs yet", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Open roles show up here. Use the button to post one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        items(jobs, key = { it.id }) { job -> 
            JobItem(job = job, onClick = { onJobClick(job) })
        }
    }
}
