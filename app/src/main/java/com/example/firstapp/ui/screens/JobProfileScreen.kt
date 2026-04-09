package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobProfileScreen(
    initialData: JobProfileData? = null,
    onClose: () -> Unit,
    onSubmit: (JobProfileData) -> Unit
) {
    // Basic Info
    var fullName by remember { mutableStateOf(initialData?.fullName ?: "") }
    var email by remember { mutableStateOf(initialData?.email ?: "") }
    var phone by remember { mutableStateOf(initialData?.phone ?: "") }
    var location by remember { mutableStateOf(initialData?.location ?: "") }
    
    // Professional Background
    var education by remember { mutableStateOf(initialData?.education ?: "") }
    var experience by remember { mutableStateOf(initialData?.experience ?: "") }
    var skills by remember { mutableStateOf(initialData?.skills ?: "") }
    var aboutMe by remember { mutableStateOf(initialData?.aboutMe ?: "") }
    
    // Professional Links
    var linkedinUrl by remember { mutableStateOf(initialData?.linkedinUrl ?: "") }
    var portfolioUrl by remember { mutableStateOf(initialData?.portfolioUrl ?: "") }
    
    // Preferences
    var workPreference by remember { mutableStateOf(initialData?.workPreference ?: "Remote") }
    var availability by remember { mutableStateOf(initialData?.availability ?: "Immediately") }
    var expectedSalary by remember { mutableStateOf(initialData?.expectedSalary ?: "") }

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
                    containerColor = AppSecondary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            Column {
                Text(
                    "Showcase your talent",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Complete your profile to get discovered by top companies.",
                    color = Color.White.copy(alpha = 0.6f),
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
            ProfileTextField(
                value = experience, 
                onValueChange = { experience = it }, 
                label = "Work Experience", 
                placeholder = "Company Name - Role (Years)\nBrief description of impact...",
                minLines = 4
            )
            ProfileTextField(
                value = skills, 
                onValueChange = { skills = it }, 
                label = "Key Skills", 
                placeholder = "e.g. Android Development, Project Management, SQL..."
            )

            SectionHeader("JOB PREFERENCES")
            
            // Work Preference (Segmented Buttons feel professional)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Work Preference", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Remote", "On-site", "Hybrid").forEach { pref ->
                        FilterChip(
                            selected = workPreference == pref,
                            onClick = { workPreference = pref },
                            label = { Text(pref) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = AppBackground,
                                selectedContainerColor = AppAccent
                            )
                        )
                    }
                }
            }

            ProfileTextField(value = expectedSalary, onValueChange = { expectedSalary = it }, label = "Expected Monthly Salary (Optional)", placeholder = "e.g. $5,000")
            ProfileTextField(value = availability, onValueChange = { availability = it }, label = "Availability", placeholder = "e.g. 2 weeks notice")

            SectionHeader("RESUME")
            OutlinedButton(
                onClick = { /* Implement file picker */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppAccent),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccent)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPLOAD CV / RESUME (PDF)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSubmit(
                        JobProfileData(
                            fullName, email, phone, location, education, experience, 
                            skills, aboutMe, linkedinUrl, portfolioUrl, 
                            workPreference, availability, expectedSalary
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE PROFESSIONAL PROFILE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppBackground)
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
            color = AppAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 1.dp, color = AppAccent.copy(alpha = 0.3f))
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
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color.Gray, fontSize = 14.sp) },
            textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppAccent,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                cursorColor = AppAccent,
                focusedContainerColor = Color.White.copy(alpha = 0.05f)
            ),
            minLines = minLines,
            shape = RoundedCornerShape(8.dp)
        )
    }
}

data class JobProfileData(
    val fullName: String,
    val email: String,
    val phone: String,
    val location: String,
    val education: String,
    val experience: String,
    val skills: String,
    val aboutMe: String,
    val linkedinUrl: String,
    val portfolioUrl: String,
    val workPreference: String,
    val availability: String,
    val expectedSalary: String
)
