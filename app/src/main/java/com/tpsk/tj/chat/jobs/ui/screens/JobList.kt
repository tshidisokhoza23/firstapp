package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tpsk.tj.common.data.ads.AdUnitConfig
import com.tpsk.tj.common.ui.components.NativeAdCard
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.home.ui.components.AppLogo
import com.tpsk.tj.ui.navigation.Screen

private val jobCategories = listOf(
    "All", "General", "IT", "Finance", "Sales", "Healthcare", "Education",
    "Engineering", "Marketing", "Hospitality", "Logistics", "Admin", "Legal", "Construction"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsHomeScreen(
    navController: NavController,
    bottomPadding: Dp = 0.dp,
    viewModel: JobViewModel = hiltViewModel()
) {
    val jobsState by viewModel.jobs.collectAsState()
    val selectedChip by viewModel.feedChip.collectAsState()
    val location by viewModel.location.collectAsState()
    val salaryMin by viewModel.salaryMin.collectAsState()
    val postedWithin by viewModel.postedWithinDays.collectAsState()
    val myJobs by viewModel.myJobs.collectAsState()
    val dateChips = listOf(0 to "Any date", 1 to "24h", 7 to "7 days", 30 to "30 days")
    var showMenu by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF)))
    Column(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
        if (searching) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Search jobs") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        searching = false
                        query = ""
                        viewModel.fetchJobs()
                    }) { Icon(Icons.Default.Close, contentDescription = "Close search") }
                }
            )
        } else {
            CenterAlignedTopAppBar(
                title = { AppLogo(size = 36.dp) },
                navigationIcon = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { searching = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(jobCategories) { chip ->
                val selected = selectedChip == chip
                Surface(
                    onClick = { viewModel.selectFeedChip(chip) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) Color.Transparent else Color(0xFF242424),
                    border = if (selected) null else BorderStroke(1.dp, Color(0xFF333333)),
                    modifier = if (selected) Modifier.background(gradient, RoundedCornerShape(20.dp)) else Modifier
                ) {
                    Text(
                        chip,
                        color = if (selected) Color.White else Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = location,
                onValueChange = viewModel::setLocation,
                label = { Text("Location") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = salaryMin,
                onValueChange = viewModel::setSalaryMin,
                label = { Text("Min salary") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dateChips.forEach { (days, label) ->
                FilterChip(selected = postedWithin == days, onClick = { viewModel.setPostedWithinDays(days) }, label = { Text(label) })
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (val state = jobsState) {
            is Resource.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.message ?: "Could not load jobs")
                    TextButton(onClick = { viewModel.fetchJobs() }) { Text("Retry") }
                }
            }
            is Resource.Loading -> {
                if (state.data.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    JobList(state.data ?: emptyList()) {
                        navController.navigate(Screen.JobDetail.createRoute(it.id))
                    }
                }
            }
            is Resource.Success -> JobList(state.data ?: emptyList(), emptyMessage = if (selectedChip == "All" && location.isBlank() && salaryMin.isBlank()) "Open roles show up here. Use the button to post one." else "Try another filter or check back later.") {
                navController.navigate(Screen.JobDetail.createRoute(it.id))
            }
        }
        }
    }
    if (showMenu) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ModalBottomSheet(onDismissRequest = { showMenu = false }) {
            ListItem(
                headlineContent = { Text("Post a job") },
                leadingContent = { Icon(Icons.Default.Add, null) },
                modifier = Modifier.clickable {
                    showMenu = false
                    navController.navigate(Screen.PostJob.route)
                }
            )
            ListItem(
                headlineContent = { Text("My applications") },
                leadingContent = { Icon(Icons.Default.Work, null) },
                modifier = Modifier.clickable {
                    showMenu = false
                    navController.navigate(Screen.Applications.route)
                }
            )
            ListItem(
                headlineContent = { Text("Job profile") },
                leadingContent = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.clickable {
                    showMenu = false
                    navController.navigate(Screen.JobProfile.route)
                }
            )
            ListItem(
                headlineContent = { Text("My listings") },
                leadingContent = { Icon(Icons.Default.Bookmark, null) },
                modifier = Modifier.clickable { viewModel.fetchMyJobs() }
            )
            val mine = (myJobs as? Resource.Success)?.data.orEmpty()
            mine.take(8).forEach { job ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(job.title, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.closeJob(job.id) }) { Text("Close") }
                    TextButton(onClick = {
                        viewModel.renewJob(job.id) { url ->
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                        }
                    }) { Text("Renew") }
                }
            }
            TextButton(onClick = { viewModel.anonymizeJobData(); showMenu = false }, modifier = Modifier.padding(16.dp)) {
                Text("Anonymize my applications")
            }
        }
    }
}

@Composable
fun JobList(jobs: List<Job>, topBarHeight: Dp = 0.dp, emptyMessage: String = "Open roles show up here. Use the button to post one.", onJobClick: (Job) -> Unit) {
    if (jobs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No jobs found", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        itemsIndexed(jobs, key = { _, job -> job.id }) { index, job ->
            JobItem(job = job, onClick = { onJobClick(job) })
            if ((index + 1) % 6 == 0) {
                NativeAdCard(adUnitId = AdUnitConfig.NATIVE_POSTS_ID)
            }
        }
    }
}
