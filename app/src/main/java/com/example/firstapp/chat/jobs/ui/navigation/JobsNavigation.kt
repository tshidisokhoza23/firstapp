package com.example.firstapp.chat.jobs.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.chat.jobs.ui.screens.*
import com.example.firstapp.chat.jobs.ui.viewmodel.*
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.common.util.Resource

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
    
    composable(Screen.Applications.route) {
        val jobApplicationsViewModel: JobApplicationsViewModel = hiltViewModel()
        JobApplicationsScreen(
            onBack = { navController.popBackStack() },
            onViewJobDetails = { jobId -> navController.navigate(Screen.JobDetail.createRoute(jobId)) },
            onContactRecruiter = { id ->
                navController.navigate(Screen.Chat.createRoute(id, type = "job_chat"))
            },
            onSetReminder = { application -> /* Handle reminder */ },
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
        
        val jobStateValue = jobState
        if (jobStateValue is Resource.Success) {
            val currentJob = jobStateValue.data!!
            JobDetailScreen(job = currentJob, onBack = { navController.popBackStack() }) {
                if (currentJob.isApplied) {
                    android.widget.Toast.makeText(context, "Already applied", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    jobDetailViewModel.applyForJob(currentJob.id, currentJob.title, currentJob.companyName)
                }
            }
        }
    }
}
