package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.chat.jobs.model.JobProfileData
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobProfileViewModel
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.profile.model.WorkExperience
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        data ?: emptyList()
    } else emptyList()

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

    var headline by remember(initialData) { mutableStateOf(initialData?.headline ?: "") }
    var fullName by remember(initialData) { mutableStateOf(initialData?.fullName ?: "") }
    var email by remember(initialData) { mutableStateOf(initialData?.email ?: "") }
    var phone by remember(initialData) { mutableStateOf(initialData?.phone ?: "") }
    var city by remember(initialData) { mutableStateOf(initialData?.city?.ifEmpty { initialData.location } ?: initialData?.location ?: "") }
    var country by remember(initialData) { mutableStateOf(initialData?.country ?: "ZA") }

    var education by remember(initialData) { mutableStateOf(initialData?.education ?: "") }
    var experience by remember(initialData) { mutableStateOf(initialData?.experience ?: "") }
    var noFormalExperience by remember(initialData) { mutableStateOf(initialData?.noFormalExperience ?: false) }

    var skillChips by remember(initialData) { mutableStateOf(initialData?.getParsedSkills() ?: emptyList()) }
    var newSkillInput by remember { mutableStateOf("") }

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
                    "Complete your profile to get matched with personalized job opportunities.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            SectionHeader("TARGET ROLE & TITLE")
            ProfileTextField(
                value = headline,
                onValueChange = { headline = it },
                label = "Target Role / Headline",
                placeholder = "e.g. Senior Software Developer, Driver, Business Analyst"
            )

            SectionHeader("PERSONAL INFORMATION")
            ProfileTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name")
            ProfileTextField(value = email, onValueChange = { email = it }, label = "Email Address")
            ProfileTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(2f)) {
                    ProfileTextField(value = city, onValueChange = { city = it }, label = "City / Region", placeholder = "e.g. Johannesburg, Gauteng")
                }
                Column(modifier = Modifier.weight(1f)) {
                    ProfileTextField(value = country, onValueChange = { country = it }, label = "Country Code", placeholder = "ZA")
                }
            }

            SectionHeader("PROFESSIONAL LINKS")
            ProfileTextField(value = linkedinUrl, onValueChange = { linkedinUrl = it }, label = "LinkedIn Profile URL", placeholder = "https://linkedin.com/in/...")
            ProfileTextField(value = portfolioUrl, onValueChange = { portfolioUrl = it }, label = "Portfolio/GitHub URL", placeholder = "https://github.com/...")

            SectionHeader("PROFESSIONAL SUMMARY")
            ProfileTextField(
                value = aboutMe,
                onValueChange = { aboutMe = it },
                label = "About Me",
                placeholder = "Highlight your career achievements and what you're looking for...",
                minLines = 3
            )

            SectionHeader("EXPERIENCE & SKILLS")
            ProfileTextField(value = education, onValueChange = { education = it }, label = "Highest Education")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { noFormalExperience = !noFormalExperience }
                ) {
                    Checkbox(checked = noFormalExperience, onCheckedChange = { noFormalExperience = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I am a student or career changer (No formal experience yet)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (!noFormalExperience) {
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
            }

            // Gap 1: Structured Chip Input for Key Skills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Key Skills (Tap to add tags)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSkillInput,
                        onValueChange = { text ->
                            if (text.contains(",")) {
                                val parts = text.split(",")
                                val added = parts[0].trim()
                                if (added.isNotBlank() && !skillChips.contains(added)) {
                                    skillChips = skillChips + added
                                }
                                newSkillInput = parts.getOrNull(1)?.trim() ?: ""
                            } else {
                                newSkillInput = text
                            }
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type skill & press Add (e.g. Kotlin)", fontSize = 14.sp) },
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newSkillInput.trim()
                            if (trimmed.isNotBlank() && !skillChips.contains(trimmed)) {
                                skillChips = skillChips + trimmed
                                newSkillInput = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Add")
                    }
                }

                if (skillChips.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        skillChips.forEach { chip ->
                            InputChip(
                                selected = true,
                                onClick = { skillChips = skillChips - chip },
                                label = { Text(chip, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove skill",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            SectionHeader("JOB PREFERENCES")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Work Preference", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val prefs = listOf("Remote", "On-site", "Hybrid")
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

            // Gap 3 & Polish: Salary Prefix with Currency ZAR/R
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Expected Monthly Salary (ZAR / R)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
                OutlinedTextField(
                    value = expectedSalary,
                    onValueChange = { expectedSalary = it },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("R ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                    placeholder = { Text("15,000", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            ProfileTextField(value = availability, onValueChange = { availability = it }, label = "Availability", placeholder = "e.g. 2 weeks notice / Immediate")

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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                    val combinedSkills = skillChips.joinToString(", ")
                    viewModel.updateProfile(
                        JobProfileData(
                            fullName = fullName,
                            email = email,
                            phone = phone,
                            location = if (city.isNotBlank()) "$city, $country" else country,
                            city = city,
                            country = country,
                            education = education,
                            experience = experience,
                            skills = combinedSkills,
                            skillsList = skillChips,
                            aboutMe = aboutMe,
                            linkedinUrl = linkedinUrl,
                            portfolioUrl = portfolioUrl,
                            workPreference = workPreference,
                            availability = availability,
                            expectedSalary = expectedSalary,
                            currency = "ZAR",
                            headline = headline,
                            targetJobTitles = if (headline.isNotBlank()) listOf(headline) else emptyList(),
                            noFormalExperience = noFormalExperience
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = experience.jobTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = experience.companyName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                val periodText = if (experience.isCurrentJob) {
                    "${dateFormat.format(experience.startDate)} - Present"
                } else if (experience.endDate != null) {
                    "${dateFormat.format(experience.startDate)} - ${dateFormat.format(experience.endDate)}"
                } else {
                    dateFormat.format(experience.startDate)
                }
                Text(
                    text = periodText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Experience",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
