package com.example.firstapp.jobs.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.jobs.ui.screens.*
import com.example.firstapp.jobs.ui.viewmodel.*
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.common.util.Resource

fun NavGraphBuilder.jobsNavGraph(navController: NavHostController, mainViewModel: MainViewModel) {
    composable(Screen.JobProfile.route) {
        JobProfileScreen(
            onClose = { navController.popBackStack() },
        )
    }
    
    composable(Screen.Applications.route) {
        val jobApplicationsViewModel: JobApplicationsViewModel = hiltViewModel()
        JobApplicationsScreen(
            onBack = { navController.popBackStack() },
            onViewJobDetails = { jobId ->
                navController.navigate(Screen.JobDetail.createRoute(jobId))
            },
            onContactRecruiter = { _ ->
                navController.navigate(Screen.Chat.createRoute(999))
            },
            onSetReminder = { application ->
                mainViewModel.requestNavigation("Reminder set for application ${application.id}")
            },
            viewModel = jobApplicationsViewModel,
        )
    }

    composable(
        route = Screen.JobDetail.route,
        arguments = listOf(navArgument("jobId") { type = NavType.IntType })
    ) { backStackEntry ->
        val jobDetailViewModel: JobDetailViewModel = hiltViewModel()
        val jobId = backStackEntry.arguments?.getInt("jobId") ?: return@composable
        LaunchedEffect(jobId) { jobDetailViewModel.loadJob(jobId) }
        
        val jobState by jobDetailViewModel.job.collectAsState()
        val applyResult by jobDetailViewModel.applyResult.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        LaunchedEffect(applyResult) {
            when (applyResult) {
                is Resource.Success -> {
                    android.widget.Toast.makeText(context, "Application submitted successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                    jobDetailViewModel.resetApplyResult()
                }
                is Resource.Error -> {
                    android.widget.Toast.makeText(context, applyResult?.message ?: "Failed to apply", android.widget.Toast.LENGTH_SHORT).show()
                    jobDetailViewModel.resetApplyResult()
                }
                else -> {}
            }
        }
        
        jobState.data?.let { job ->
            JobDetailScreen(
                job = job,
                onBack = { navController.popBackStack() },
            ) {
                if (job.isApplied) {
                    android.widget.Toast.makeText(
                        context,
                        "You have already applied for this job",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    jobDetailViewModel.applyForJob(job.id)
                }
            }
        }
    }
}
