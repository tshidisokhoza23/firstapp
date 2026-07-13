package com.example.firstapp.profile.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.profile.model.WorkExperience
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkExperienceScreen(
    experienceId: String? = null,
    onBack: () -> Unit,
    viewModel: UserViewModel
) {
    val context = LocalContext.current
    val workExperiencesResource by viewModel.workExperiences.collectAsState()
    val updateState by viewModel.updateWorkState.collectAsState()
    
    val initialData: WorkExperience? = remember(experienceId, workExperiencesResource) {
        val res = workExperiencesResource
        if (experienceId != null && res is Resource.Success) {
            val list = res.data
            var found: WorkExperience? = null
            if (list != null) {
                for (item in list) {
                    if (item.id == experienceId) {
                        found = item
                        break
                    }
                }
            }
            found
        } else null
    }

    var jobTitle by remember(initialData) { mutableStateOf(initialData?.jobTitle ?: "") }
    var companyName by remember(initialData) { mutableStateOf(initialData?.companyName ?: "") }
    var experienceLocation by remember(initialData) { mutableStateOf(initialData?.location ?: "") }
    var employmentType by remember(initialData) { mutableStateOf(initialData?.employmentType ?: "Full-time") }
    var isCurrentJob by remember(initialData) { mutableStateOf(initialData?.isCurrentJob ?: false) }
    var startDate by remember(initialData) { mutableLongStateOf(initialData?.startDate ?: System.currentTimeMillis()) }
    var endDate by remember(initialData) { mutableStateOf<Long?>(initialData?.endDate) }
    var description by remember(initialData) { mutableStateOf(initialData?.description ?: "") }

    var expanded by remember { mutableStateOf(false) }
    val employmentTypes = kotlin.collections.listOf("Full-time", "Part-time", "Contract", "Freelance", "Internship")

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val isLoading = updateState is Resource.Loading<*>
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateState) {
        val state = updateState
        if (state is Resource.Success<*>) {
            viewModel.clearUpdateWorkState()
            onBack()
        } else if (state is Resource.Error<*>) {
            snackbarHostState.showSnackbar(state.message ?: "Error occurred")
            viewModel.clearUpdateWorkState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (experienceId == null) "Add Experience" else "Edit Experience", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    val canSave = !isLoading && jobTitle.trim().isNotEmpty() && companyName.trim().isNotEmpty()
                    TextButton(
                        onClick = {
                            if (jobTitle.trim().isEmpty() || companyName.trim().isEmpty()) return@TextButton
                            
                            val exp = WorkExperience(
                                id = experienceId,
                                jobTitle = jobTitle,
                                companyName = companyName,
                                location = if (experienceLocation.trim().isNotEmpty()) experienceLocation else null,
                                employmentType = employmentType,
                                isCurrentJob = isCurrentJob,
                                startDate = startDate,
                                endDate = if (isCurrentJob) null else endDate,
                                description = if (description.trim().isNotEmpty()) description else null
                            )
                            
                            if (experienceId == null) {
                                viewModel.addWorkExperience(exp)
                            } else {
                                viewModel.updateWorkExperience(exp)
                            }
                        },
                        enabled = canSave
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AppAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            val btnColor = if (canSave) AppAccent else Color.Gray
                            Text("Save", color = btnColor, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = jobTitle,
                onValueChange = { if (it.length <= 100) jobTitle = it },
                label = { Text("Job Title") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = AppAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
            )

            OutlinedTextField(
                value = companyName,
                onValueChange = { if (it.length <= 100) companyName = it },
                label = { Text("Company Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = AppAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
            )

            OutlinedTextField(
                value = experienceLocation,
                onValueChange = { if (it.length <= 100) experienceLocation = it },
                label = { Text("Location") },
                placeholder = { Text("e.g. Johannesburg or Remote") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = AppAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = employmentType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Employment Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = AppAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    for (type in employmentTypes) {
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                employmentType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Is Current Job?", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = isCurrentJob,
                    onCheckedChange = { 
                        isCurrentJob = it
                        if (it) endDate = null
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)
                )
            }

            OutlinedTextField(
                value = dateFormat.format(Date(startDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = startDate
                        DatePickerDialog(context, { _, y, m, d ->
                            val newCal = Calendar.getInstance()
                            newCal.set(y, m, d)
                            if (!newCal.time.after(Date())) {
                                startDate = newCal.timeInMillis
                            }
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, null, tint = AppAccent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = AppAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
            )

            val currentEnd = endDate
            OutlinedTextField(
                value = if (currentEnd != null) dateFormat.format(Date(currentEnd)) else "",
                onValueChange = {},
                readOnly = true,
                enabled = !isCurrentJob,
                label = { Text("End Date") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        val cEnd = endDate
                        cal.timeInMillis = if (cEnd != null) cEnd else System.currentTimeMillis()
                        DatePickerDialog(context, { _, y, m, d ->
                            val newCal = Calendar.getInstance()
                            newCal.set(y, m, d)
                            if (newCal.timeInMillis > startDate) {
                                endDate = newCal.timeInMillis
                            }
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    }, enabled = !isCurrentJob) {
                        Icon(Icons.Default.CalendarToday, null, tint = if (isCurrentJob) Color.Gray else AppAccent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, 
                    unfocusedTextColor = Color.White, 
                    disabledTextColor = Color.Gray,
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    disabledBorderColor = Color.White.copy(alpha = 0.05f)
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 2000) description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = AppAccent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f))
            )
        }
    }
}
