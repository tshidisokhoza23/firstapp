package com.example.firstapp.chat.jobs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.chat.jobs.ui.components.JobItem

@Composable
fun JobList(jobs: List<Job>, topBarHeight: Dp = 0.dp, onJobClick: (Job) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { Spacer(modifier = Modifier.height(topBarHeight)) }
        items(jobs, key = { it.id }) { job -> 
            JobItem(job = job, onClick = { onJobClick(job) })
        }
    }
}
