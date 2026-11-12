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
    val activeTab by viewModel.activeTab.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val mainTabs = listOf("All", "For You", "Recent", "Saved")

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
                CenterAlignedTopAppBar(
                    title = { AppLogo(size = 36.dp) },
                    navigationIcon = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("My jobs") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.MyJobs.route)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Applications") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.Applications.route)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Job profile") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.JobProfile.route)
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.JobSearch.route) }) {
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

            // High-intent discovery tab chips row
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mainTabs) { tab ->
                    val isSelected = (activeTab == tab)
                    com.tpsk.tj.common.ui.components.AppFilterChip(
                        text = tab,
                        isSelected = isSelected,
                        onClick = {
                            viewModel.selectFeedTab(tab)
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
                                jobs = state.data ?: emptyList(),
                                activeTab = activeTab,
                                onBrowseAllClick = { viewModel.selectFeedTab("All") },
                                onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                                onSaveClick = { viewModel.toggleSave(it) }
                            )
                        }
                    }
                    is Resource.Success -> {
                        JobList(
                            jobs = state.data ?: emptyList(),
                            activeTab = activeTab,
                            onBrowseAllClick = { viewModel.selectFeedTab("All") },
                            onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                            onSaveClick = { viewModel.toggleSave(it) }
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

@Composable
fun JobList(
    jobs: List<Job>,
    topBarHeight: Dp = 0.dp,
    activeTab: String = "All",
    onBrowseAllClick: () -> Unit = {},
    onJobClick: (Job) -> Unit,
    onSaveClick: (Job) -> Unit = {}
) {
    if (jobs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (activeTab) {
                "For You" -> {
                    Text(
                        text = "No personalized matches yet",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete your job profile or browse All jobs for personalized matches.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBrowseAllClick) {
                        Text("Browse All Jobs")
                    }
                }
                "Saved" -> {
                    Text(
                        text = "No saved jobs yet",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the bookmark icon on any job listing to save it for later.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = "No jobs yet",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Open roles show up here. Use the button to post one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        items(jobs, key = { it.id }) { job ->
            JobItem(job = job, onClick = { onJobClick(job) }, onSaveClick = { onSaveClick(job) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(
    onBack: () -> Unit,
    onJobClick: (String) -> Unit,
    viewModel: JobViewModel = hiltViewModel()
) {
    val state by viewModel.myJobs.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { viewModel.fetchMyJobs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My jobs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val current = state) {
            is Resource.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(current.message ?: "Could not load your jobs")
                    Button(onClick = { viewModel.fetchMyJobs() }) { Text("Retry") }
                }
            }
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Success -> {
                val jobs = current.data.orEmpty()
                if (jobs.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("You have not posted a job yet")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(jobs, key = { it.id }) { job ->
                            Column(modifier = Modifier.fillMaxWidth().clickable { onJobClick(job.id) }) {
                                Text(job.title, fontWeight = FontWeight.Bold)
                                Text("${job.companyName} · ${job.listingStatus ?: "posted"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { viewModel.closeJob(job.id) }) { Text("Close") }
                                    TextButton(onClick = {
                                        viewModel.renewJob(job.id) { url ->
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                        }
                                    }) { Text("Renew") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
