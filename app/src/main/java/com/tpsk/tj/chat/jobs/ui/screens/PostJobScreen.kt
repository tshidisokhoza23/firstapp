package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.chat.jobs.model.JobTemplate
import com.tpsk.tj.chat.jobs.model.JobTemplatesMock
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobViewModel

enum class PostJobStep {
    TEMPLATE_SELECTION,
    TEMPLATE_PREVIEW,
    EDIT_TEMPLATE,
    JOB_DETAILS,
    REVIEW_LISTING,
    SUCCESS
}

class JobCreationState {
    var title by mutableStateOf("")
    var company by mutableStateOf("")
    var category by mutableStateOf("General Labour")
    var employmentType by mutableStateOf("Permanent")
    var location by mutableStateOf("")
    var salaryRange by mutableStateOf("")
    var description by mutableStateOf("")
    var responsibilities by mutableStateOf<List<String>>(emptyList())
    var requirements by mutableStateOf<List<String>>(emptyList())
    var isFromScratch by mutableStateOf(false)

    fun loadFromTemplate(template: JobTemplate) {
        title = template.title
        description = template.description
        employmentType = template.employmentType
        responsibilities = template.responsibilities
        requirements = template.requirements
        isFromScratch = false
    }

    fun clear() {
        title = ""
        company = ""
        category = "General Labour"
        employmentType = "Permanent"
        location = ""
        salaryRange = ""
        description = ""
        responsibilities = emptyList()
        requirements = emptyList()
        isFromScratch = true
    }
}

@Composable
fun PostJobScreen(
    onBack: () -> Unit,
    viewModel: JobViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var currentStep by remember { mutableStateOf(PostJobStep.TEMPLATE_SELECTION) }
    val jobState = remember { JobCreationState() }

    val createResult by viewModel.createJobResult.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(createResult) {
        val result = createResult
        when (result) {
            is com.tpsk.tj.common.util.Resource.Success -> {
                val url = result.data?.authorizationUrl
                if (!url.isNullOrBlank()) {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }
                currentStep = PostJobStep.SUCCESS
                viewModel.clearCreateResult()
            }
            is com.tpsk.tj.common.util.Resource.Error -> {
                android.widget.Toast.makeText(context, result.message ?: "Could not post job", android.widget.Toast.LENGTH_LONG).show()
                viewModel.clearCreateResult()
            }
            else -> {}
        }
    }

    when (currentStep) {
        PostJobStep.TEMPLATE_SELECTION -> {
            TemplateSelectionScreen(
                onBack = onBack,
                onTemplateSelected = {
                    jobState.loadFromTemplate(it)
                    currentStep = PostJobStep.TEMPLATE_PREVIEW
                },
                onStartFromScratch = {
                    jobState.clear()
                    currentStep = PostJobStep.JOB_DETAILS
                }
            )
        }
        PostJobStep.TEMPLATE_PREVIEW -> {
            TemplatePreviewScreen(
                jobState = jobState,
                onBack = { currentStep = PostJobStep.TEMPLATE_SELECTION },
                onEditTemplate = { currentStep = PostJobStep.EDIT_TEMPLATE },
                onUseTemplate = { currentStep = PostJobStep.JOB_DETAILS }
            )
        }
        PostJobStep.EDIT_TEMPLATE -> {
            EditTemplateScreen(
                jobState = jobState,
                onCancel = {
                    if (jobState.isFromScratch) {
                        currentStep = PostJobStep.JOB_DETAILS
                    } else {
                        currentStep = PostJobStep.TEMPLATE_PREVIEW
                    }
                },
                onSave = { currentStep = PostJobStep.JOB_DETAILS }
            )
        }
        PostJobStep.JOB_DETAILS -> {
            JobDetailsScreen(
                jobState = jobState,
                onBack = {
                    if (jobState.isFromScratch) currentStep = PostJobStep.TEMPLATE_SELECTION
                    else currentStep = PostJobStep.TEMPLATE_PREVIEW
                },
                onEditSection = { currentStep = PostJobStep.EDIT_TEMPLATE },
                onNext = { currentStep = PostJobStep.REVIEW_LISTING }
            )
        }
        PostJobStep.REVIEW_LISTING -> {
            ReviewListingScreen(
                jobState = jobState,
                onBack = { currentStep = PostJobStep.JOB_DETAILS },
                onPublish = {
                    val formattedRequirements = jobState.requirements.filter { it.isNotBlank() }.joinToString(separator = "\n") { "- $it" }
                    val formattedResponsibilities = jobState.responsibilities.filter { it.isNotBlank() }.joinToString(separator = "\n") { "- $it" }

                    var finalDescription = jobState.description
                    if (formattedResponsibilities.isNotBlank()) {
                        finalDescription += "\n\nResponsibilities:\n$formattedResponsibilities"
                    }
                    if (formattedRequirements.isNotBlank()) {
                        finalDescription += "\n\nRequirements:\n$formattedRequirements"
                    }

                    viewModel.createJob(
                        title = jobState.title,
                        description = finalDescription,
                        category = jobState.category,
                        skills = jobState.requirements.filter { it.isNotBlank() },
                        durationDays = 30,
                        company = jobState.company,
                        locationCity = jobState.location,
                        packageType = "basic",
                        requirements = formattedRequirements
                    )
                }
            )
        }
        PostJobStep.SUCCESS -> {
            SuccessScreen(
                jobState = jobState,
                onViewJobs = onBack,
                onPostAnother = {
                    jobState.clear()
                    currentStep = PostJobStep.TEMPLATE_SELECTION
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionScreen(
    onBack: () -> Unit,
    onTemplateSelected: (JobTemplate) -> Unit,
    onStartFromScratch: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Popular") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Job", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create a Job Listing",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "How would you like to start?",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search templates...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Popular", "All", "Trades", "Office").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            val filteredTemplates = JobTemplatesMock.templates.filter {
                it.title.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredTemplates) { template ->
                    val icon = when (template.title) {
                        "General Worker" -> Icons.Default.Construction
                        "Driver" -> Icons.Default.DirectionsCar
                        "Security Guard" -> Icons.Default.Security
                        "Waiter / Waitress" -> Icons.Default.Restaurant
                        "IT Support" -> Icons.Default.Computer
                        else -> Icons.Default.Work
                    }

                    Card(
                        onClick = { onTemplateSelected(template) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Description + requirements", fontSize = 14.sp, color = Color.Gray)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onStartFromScratch,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start from scratch")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePreviewScreen(
    jobState: JobCreationState,
    onBack: () -> Unit,
    onEditTemplate: () -> Unit,
    onUseTemplate: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Template Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface, contentPadding = PaddingValues(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onEditTemplate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Template")
                    }
                    Button(
                        onClick = onUseTemplate,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Use This Template ->")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF1E3A8A)), // Deep blue placeholder
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(jobState.title, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(jobState.employmentType, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subject, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Job Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(jobState.description, color = Color.DarkGray)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Responsibilities", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        jobState.responsibilities.forEach {
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(modifier = Modifier.padding(top = 6.dp, end = 8.dp).size(6.dp).background(Color.Gray, CircleShape))
                                Text(it, color = Color.DarkGray)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Requirements", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        jobState.requirements.forEach {
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                Box(modifier = Modifier.padding(top = 6.dp, end = 8.dp).size(6.dp).background(Color.Gray, CircleShape))
                                Text(it, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTemplateScreen(
    jobState: JobCreationState,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    // Local state for editing
    var description by remember { mutableStateOf(jobState.description) }
    var responsibilities by remember { mutableStateOf(jobState.responsibilities) }
    var requirements by remember { mutableStateOf(jobState.requirements) }
    
    val maxItems = 15

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface, contentPadding = PaddingValues(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            jobState.description = description
                            jobState.responsibilities = responsibilities
                            jobState.requirements = requirements
                            onSave()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Template")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(jobState.title.ifEmpty { "Custom Listing" }, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("Edit the template details below.", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Job Description *", fontWeight = FontWeight.Bold)
                    Text("${description.length}/2000", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 2000) description = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Responsibilities *", fontWeight = FontWeight.Bold)
                    Text("${responsibilities.joinToString("").length}/1000", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                responsibilities.forEachIndexed { index, resp ->
                    DynamicListItem(
                        text = resp,
                        onTextChange = { newText ->
                            val newList = responsibilities.toMutableList()
                            newList[index] = newText
                            responsibilities = newList
                        },
                        onRemove = {
                            val newList = responsibilities.toMutableList()
                            newList.removeAt(index)
                            responsibilities = newList
                        }
                    )
                }
                if (responsibilities.size < maxItems) {
                    TextButton(onClick = {
                        val newList = responsibilities.toMutableList()
                        newList.add("")
                        responsibilities = newList
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add responsibility")
                    }
                }
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Requirements *", fontWeight = FontWeight.Bold)
                    Text("${requirements.joinToString("").length}/1000", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                requirements.forEachIndexed { index, req ->
                    DynamicListItem(
                        text = req,
                        onTextChange = { newText ->
                            val newList = requirements.toMutableList()
                            newList[index] = newText
                            requirements = newList
                        },
                        onRemove = {
                            val newList = requirements.toMutableList()
                            newList.removeAt(index)
                            requirements = newList
                        }
                    )
                }
                if (requirements.size < maxItems) {
                    TextButton(onClick = {
                        val newList = requirements.toMutableList()
                        newList.add("")
                        requirements = newList
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add requirement")
                    }
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Need more options?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Browse more templates or add your own.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicListItem(
    text: String,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            singleLine = false,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(32.dp)
                .border(1.dp, Color.Red, CircleShape)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    jobState: JobCreationState,
    onBack: () -> Unit,
    onEditSection: () -> Unit,
    onNext: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Job", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface, contentPadding = PaddingValues(16.dp)) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next: Review Listing ->")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Job Details", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            OutlinedTextField(
                value = jobState.title,
                onValueChange = { jobState.title = it },
                label = { Text("Job Title *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobState.company,
                onValueChange = { jobState.company = it },
                label = { Text("Company *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobState.category,
                onValueChange = { jobState.category = it },
                label = { Text("Category *") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobState.employmentType,
                onValueChange = { jobState.employmentType = it },
                label = { Text("Employment Type *") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobState.location,
                onValueChange = { jobState.location = it },
                label = { Text("Location *") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = jobState.salaryRange,
                onValueChange = { jobState.salaryRange = it },
                label = { Text("Salary Range (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                val sections = listOf(
                    "Job Description" to jobState.description.isNotEmpty(),
                    "Responsibilities" to jobState.responsibilities.isNotEmpty(),
                    "Requirements" to jobState.requirements.isNotEmpty()
                )
                
                sections.forEach { (label, hasContent) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditSection() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (hasContent) Icons.Default.Check else Icons.Default.Add, 
                            contentDescription = null, 
                            tint = if (hasContent) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Edit", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewListingScreen(
    jobState: JobCreationState,
    onBack: () -> Unit,
    onPublish: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Your Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surface, contentPadding = PaddingValues(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = onPublish,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Post Job (R119)")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview Your Job", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text(jobState.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Active", color = Color(0xFF2E7D32), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val details = listOf(
                        Icons.Default.Business to jobState.company,
                        Icons.Default.LocationOn to jobState.location,
                        Icons.Default.Payments to (jobState.salaryRange.ifEmpty { "Not specified" }),
                        Icons.Default.Event to jobState.employmentType
                    )
                    
                    details.forEach { (icon, text) ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text, color = Color.DarkGray, fontSize = 14.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Job Description", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(jobState.description, color = Color.DarkGray, fontSize = 14.sp)

                    if (jobState.responsibilities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Responsibilities", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        jobState.responsibilities.forEach {
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(it, color = Color.DarkGray, fontSize = 14.sp)
                            }
                        }
                    }

                    if (jobState.requirements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Requirements", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        jobState.requirements.forEach {
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(it, color = Color.DarkGray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessScreen(
    jobState: JobCreationState,
    onViewJobs: () -> Unit,
    onPostAnother: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment & Publish", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onViewJobs) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF4CAF50), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Job Posted Successfully!", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Your job listing is now live and visible to thousands of job seekers on TJ.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Work, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(jobState.title, fontWeight = FontWeight.Bold)
                        Text(jobState.location, color = Color.Gray, fontSize = 14.sp)
                    }
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Active", color = Color(0xFF2E7D32), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text("What's Next?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                val nextSteps = listOf(
                    "Receive applications",
                    "Chat with candidates",
                    "Update application status"
                )
                
                nextSteps.forEach { step ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(step, color = Color.DarkGray)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onViewJobs,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("View My Jobs ->")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onPostAnother,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Post Another Job")
            }
        }
    }
}
