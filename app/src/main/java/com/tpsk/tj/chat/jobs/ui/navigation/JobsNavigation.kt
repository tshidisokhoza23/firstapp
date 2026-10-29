package com.tpsk.tj.chat.jobs.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tpsk.tj.chat.jobs.ui.screens.*
import com.tpsk.tj.chat.jobs.ui.viewmodel.*
import androidx.compose.foundation.layout.fillMaxSize
import com.tpsk.tj.ui.navigation.Screen
import com.tpsk.tj.home.ui.viewmodel.MainViewModel
import com.tpsk.tj.common.util.Resource

fun NavGraphBuilder.jobsNavGraph(navController: NavHostController, mainViewModel: MainViewModel) {
    composable(Screen.JobProfile.route) {
        JobProfileScreen(
            onClose = { navController.popBackStack() },
            onNavigateToEditExperience = { experienceId ->
                navController.navigate(Screen.EditWorkExperience.createRoute(experienceId))
            }
        )
    }

    composable(Screen.PostJob.route) {
        PostJobScreen(onBack = { navController.popBackStack() })
    }

    composable(Screen.MyJobs.route) {
        MyJobsScreen(
            onBack = { navController.popBackStack() },
            onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it)) }
        )
    }
    
    composable(Screen.Applications.route) {
        val jobApplicationsViewModel: JobApplicationsViewModel = hiltViewModel()
        JobApplicationsScreen(
            onBack = { navController.popBackStack() },
            onViewJobDetails = { jobId -> navController.navigate(Screen.JobDetail.createRoute(jobId)) },
            onContactRecruiter = { id ->
                if (id.isBlank()) {
                    android.widget.Toast.makeText(
                        navController.context,
                        "No recruiter chat for this application yet",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    navController.navigate(Screen.Chat.createRoute(id, type = "job_chat"))
                }
            },
            onSetReminder = { application ->
                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                    data = android.provider.CalendarContract.Events.CONTENT_URI
                    putExtra(android.provider.CalendarContract.Events.TITLE, "Follow up: ${application.jobTitle}")
                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, application.companyName)
                }
                runCatching { navController.context.startActivity(intent) }
            },
            viewModel = jobApplicationsViewModel
        )
    }

    composable(
        route = Screen.JobDetail.route,
        arguments = listOf(navArgument("jobId") { type = NavType.StringType })
    ) { backStackEntry ->
        val jobDetailViewModel: JobDetailViewModel = hiltViewModel()
        val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
        LaunchedEffect(jobId) { jobDetailViewModel.loadJob(jobId) }
        
        val jobState by jobDetailViewModel.job.collectAsState()
        val applyResult by jobDetailViewModel.applyResult.collectAsState()
        val applyContext by jobDetailViewModel.lastApplyContext.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        LaunchedEffect(applyResult) {
            when (applyResult) {
                is Resource.Success -> {
                    val conversationId = (applyResult as Resource.Success<String>).data
                    if (conversationId != null) {
                        android.widget.Toast.makeText(context, "Application submitted!", android.widget.Toast.LENGTH_SHORT).show()
                        navController.navigate(
                            Screen.Chat.createRoute(
                                chatId = conversationId,
                                jobId = applyContext?.jobId,
                                jobTitle = applyContext?.jobTitle,
                                companyName = applyContext?.companyName
                            )
                        ) {
                            popUpTo(Screen.JobDetail.route) { inclusive = true }
                        }
                    }
                    jobDetailViewModel.resetApplyResult()
                }
                is Resource.Error -> {
                    android.widget.Toast.makeText(context, (applyResult as Resource.Error).message ?: "Failed to apply", android.widget.Toast.LENGTH_SHORT).show()
                    jobDetailViewModel.resetApplyResult()
                }
                else -> {}
            }
        }
        
        when (val jobStateValue = jobState) {
            is Resource.Success -> {
                val currentJob = jobStateValue.data ?: return@composable
                JobDetailScreen(
                    job = currentJob,
                    onBack = { navController.popBackStack() },
                    onSave = { jobDetailViewModel.toggleSave() },
                    onExternalApplyClick = { id ->
                        jobDetailViewModel.markClickedExternal(id)
                    },
                    onReportJob = { id, reason -> jobDetailViewModel.reportJob(id, reason) },
                    onApply = { note ->
                        val externalUrl = applicantDestination(currentJob.trackingApplyUrl)
                        if (currentJob.isExternal && externalUrl != null) {
                            jobDetailViewModel.markClickedExternal(currentJob.id)
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, externalUrl))
                        } else if (currentJob.isApplied || currentJob.hasAppliedInternal) {
                            android.widget.Toast.makeText(context, "Already applied", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            jobDetailViewModel.applyForJob(currentJob.id, currentJob.title, currentJob.companyName, note)
                        }
                    }
                )
            }
            is Resource.Error -> {
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Text(jobStateValue.message ?: "Could not load this job")
                    androidx.compose.material3.Button(onClick = { jobDetailViewModel.loadJob(jobId) }) {
                        androidx.compose.material3.Text("Retry")
                    }
                }
            }
            else -> {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }
    }
}

private fun applicantDestination(raw: String?): android.net.Uri? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    val uri = when {
        value.startsWith("http://") || value.startsWith("https://") || value.startsWith("mailto:") || value.startsWith("tel:") -> value
        value.contains("@") -> "mailto:$value"
        value.startsWith("+") || value.count { it.isDigit() } >= 9 -> "https://wa.me/${value.filter { it.isDigit() }}"
        else -> return null
    }
    return android.net.Uri.parse(uri)
}
