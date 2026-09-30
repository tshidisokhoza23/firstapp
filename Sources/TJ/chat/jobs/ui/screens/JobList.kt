package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.ui.components.JobItem

@Composable
fun JobList(jobs: List<Job>, topBarHeight: Dp = 0.dp, onJobClick: (Job) -> Unit) {
    if (jobs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No jobs yet", color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Open roles show up here. Use the button to post one.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
