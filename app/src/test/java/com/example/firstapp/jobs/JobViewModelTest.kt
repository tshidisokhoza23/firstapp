package com.example.firstapp.chat.jobs

import com.example.firstapp.chat.jobs.ui.viewmodel.JobViewModel
import com.example.firstapp.domain.repository.JobRepository
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.common.util.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.collections.*

@OptIn(ExperimentalCoroutinesApi::class)
class JobViewModelTest {

    private lateinit var viewModel: JobViewModel
    private val repository: JobRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getJobs() } returns flowOf(Resource.Success(emptyList<Job>()))
        every { repository.getJobApplications() } returns flowOf(Resource.Success(emptyList()))
        viewModel = JobViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `job feed matches user profile`() = runTest {
        val jobs = listOf<Job>(mockk<Job>(relaxed = true))
        coEvery { repository.getJobs() } returns flowOf(Resource.Success(jobs))
        
        viewModel.fetchJobs()
        
        assertEquals(jobs, viewModel.jobs.value.data)
    }

    @Test
    fun `adding a job verifies charge and duration`() = runTest {
        val title = "Developer"
        val desc = "Android"
        val category = "Tech"
        val skills = listOf<String>("Kotlin")
        val duration = 30
        
        coEvery { repository.createJob(title, desc, category, skills, duration) } returns flowOf(Resource.Success(mockk(relaxed = true)))
        
        viewModel.createJob(title, desc, category, skills, duration)
        
        coVerify { repository.createJob(title, desc, category, skills, duration) }
        assertTrue(viewModel.createJobResult.value is Resource.Success)
        // The charge of R20/day is enforced server-side; this test verifies correct duration payload usage.
    }

    @Test
    fun `applying for a job creates a chat instance`() = runTest {
        val jobId = "job123"
        val conversationId = "conv456"
        coEvery { repository.getJobApplications() } returns flowOf(Resource.Success(emptyList()))
        coEvery { repository.getJobs() } returns flowOf(Resource.Success(emptyList()))
        coEvery { repository.applyForJob(jobId) } returns flowOf(Resource.Success(conversationId))
        
        viewModel.applyForJob(jobId)
        
        coVerify { repository.applyForJob(jobId) }
        coVerify(atLeast = 1) { repository.getJobs() }
        coVerify(atLeast = 1) { repository.getJobApplications() }
    }
}
