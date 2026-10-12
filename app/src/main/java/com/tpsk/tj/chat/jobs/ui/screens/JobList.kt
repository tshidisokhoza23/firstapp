package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.ui.components.JobItem
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobViewModel
import com.tpsk.tj.common.data.ads.AdUnitConfig
import com.tpsk.tj.common.ui.components.NativeAdCard
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.ui.navigation.Screen

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
    val chips = listOf("All", "Full-time", "Part-time", "Remote", "Featured")
    val dateChips = listOf(0 to "Any date", 1 to "24h", 7 to "7 days", 30 to "30 days")
    Column(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
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
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { chip ->
                FilterChip(
                    selected = selectedChip == chip,
                    onClick = { viewModel.selectFeedChip(chip) },
                    label = { Text(chip) }
                )
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
        val context = androidx.compose.ui.platform.LocalContext.current
        TextButton(onClick = { viewModel.fetchMyJobs() }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("My listings") }
        TextButton(onClick = { viewModel.anonymizeJobData() }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Anonymize my applications") }
        val mine = (myJobs as? Resource.Success)?.data.orEmpty()
        mine.take(5).forEach { job ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(job.title, modifier = Modifier.weight(1f))
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
