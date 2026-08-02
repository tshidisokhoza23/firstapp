package com.example.firstapp.chat.jobs.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.chat.jobs.model.JobProfileData
import com.example.firstapp.chat.jobs.ui.viewmodel.JobProfileViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.firstapp.profile.model.WorkExperience
import kotlin.collections.List
import kotlin.collections.emptyList
import kotlin.collections.sortedWith
import kotlin.comparisons.compareByDescending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobProfileScreen(
    onClose: () -> Unit,
    onNavigateToEditExperience: (String?) -> Unit = {},
    viewModel: JobProfileViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val profileResource by viewModel.profile.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()

    val workExperiencesResource by userViewModel.workExperiences.collectAsState()
    val workExperiences: List<WorkExperience> = if (workExperiencesResource is Resource.Success) {
        val data = (workExperiencesResource as Resource.Success<List<WorkExperience>>).data
        data ?: emptyList<WorkExperience>()
    } else emptyList<WorkExperience>()

    val sortedExperiences: List<WorkExperience> = remember(workExperiences) {
        workExperiences.sortedWith(compareByDescending<WorkExperience> { it.isCurrentJob }
            .thenByDescending { it.startDate })
    }

    LaunchedEffect(saveResult) {
        if (saveResult is Resource.Success<*>) {
            onClose()
            viewModel.resetState()
        }
    }

    val initialData = (profileResource as? Resource.Success)?.data
    
    var fullName by remember(initialData) { mutableStateOf(initialData?.fullName ?: "") }
    var email by remember(initialData) { mutableStateOf(initialData?.email ?: "") }
    var phone by remember(initialData) { mutableStateOf(initialData?.phone ?: "") }
    var location by remember(initialData) { mutableStateOf(initialData?.location ?: "") }
    
    var education by remember(initialData) { mutableStateOf(initialData?.education ?: "") }
    var experience by remember(initialData) { mutableStateOf(initialData?.experience ?: "") }
    var skills by remember(initialData) { mutableStateOf(initialData?.skills ?: "") }
    var aboutMe by remember(initialData) { mutableStateOf(initialData?.aboutMe ?: "") }
    
    var linkedinUrl by remember(initialData) { mutableStateOf(initialData?.linkedinUrl ?: "") }
    var portfolioUrl by remember(initialData) { mutableStateOf(initialData?.portfolioUrl ?: "") }
    
    var workPreference by remember(initialData) { mutableStateOf(initialData?.workPreference ?: "Remote") }
    var availability by remember(initialData) { mutableStateOf(initialData?.availability ?: "Immediately") }
    var expectedSalary by remember(initialData) { mutableStateOf(initialData?.expectedSalary ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Professional Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                Text(
                    "Showcase your talent",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Complete your profile to get discovered by top companies.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            SectionHeader("PERSONAL INFORMATION")
            ProfileTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name")
            ProfileTextField(value = email, onValueChange = { email = it }, label = "Email Address")
            ProfileTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")
            ProfileTextField(value = location, onValueChange = { location = it }, label = "Current Location (City, Country)")

            SectionHeader("PROFESSIONAL LINKS")
            ProfileTextField(value = linkedinUrl, onValueChange = { linkedinUrl = it }, label = "LinkedIn Profile URL", placeholder = "https://linkedin.com/in/...")
            ProfileTextField(value = portfolioUrl, onValueChange = { portfolioUrl = it }, label = "Portfolio/GitHub URL", placeholder = "https://github.com/...")

            SectionHeader("PROFESSIONAL SUMMARY")
            ProfileTextField(
                value = aboutMe, 
                onValueChange = { aboutMe = it }, 
                label = "About Me", 
                placeholder = "Highlight your career achievements and what you're looking for...",
                minLines = 4
            )

            SectionHeader("EXPERIENCE & SKILLS")
            ProfileTextField(value = education, onValueChange = { education = it }, label = "Highest Education")
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Work Experience", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    IconButton(onClick = { onNavigateToEditExperience(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Experience", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (sortedExperiences.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "No experience added yet. Click + to add your roles.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    for (exp in sortedExperiences) {
                        ExperienceItem(
                            experience = exp,
                            onEditClick = { onNavigateToEditExperience(exp.id) }
                        )
                    }
                }
            }

            ProfileTextField(
                value = skills, 
                onValueChange = { skills = it }, 
                label = "Key Skills", 
                placeholder = "e.g. Android Development, Project Management, SQL..."
            )

            SectionHeader("JOB PREFERENCES")
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Work Preference", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val prefs = kotlin.collections.listOf("Remote", "On-site", "Hybrid")
                    for (pref in prefs) {
                        FilterChip(
                            selected = workPreference == pref,
                            onClick = { workPreference = pref },
                            label = { Text(pref) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            ProfileTextField(value = expectedSalary, onValueChange = { expectedSalary = it }, label = "Expected Monthly Salary (Optional)", placeholder = "e.g. $5,000")
            ProfileTextField(value = availability, onValueChange = { availability = it }, label = "Availability", placeholder = "e.g. 2 weeks notice")

            SectionHeader("RESUME")
            val context = androidx.compose.ui.platform.LocalContext.current
            val pdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) { 
                    android.widget.Toast.makeText(context, "Resume uploaded!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            OutlinedButton(
                onClick = { pdfLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPLOAD CV / RESUME (PDF)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.updateProfile(
                        JobProfileData(
                            fullName, email, phone, location, education, experience, 
                            skills, aboutMe, linkedinUrl, portfolioUrl, 
                            workPreference, availability, expectedSalary
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE PROFESSIONAL PROFILE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ),
            minLines = minLines,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ExperienceItem(
    experience: WorkExperience,
    onEditClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = experience.jobTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${experience.companyName} • ${experience.employmentType}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                val endDateStr = experience.endDate?.let { dateFormat.format(Date(it)) } ?: ""
                Text(
                    text = "${dateFormat.format(Date(experience.startDate))} - ${if (experience.isCurrentJob) "Present" else endDateStr}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
