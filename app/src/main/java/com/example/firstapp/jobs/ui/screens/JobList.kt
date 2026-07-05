package com.example.firstapp.jobs.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.firstapp.jobs.model.Job
import com.example.firstapp.jobs.ui.components.JobItem

@Composable
fun JobList(jobs: List<Job>, onJobClick: (Job) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 180.dp)) {
        items(jobs, key = { it.id }) { job -> 
            JobItem(job = job, onClick = { onJobClick(job) })
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp, end = 16.dp), thickness = 0.5.dp, color = Color(0xFF233138)) 
        }
    }
}
