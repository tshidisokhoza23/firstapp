@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobViewModel
import com.tpsk.tj.common.ui.components.AppFilterChip
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun JobSearchScreen(
    navController: NavController,
    viewModel: JobViewModel = hiltViewModel()
) {
    val searchResultsState by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.searchCategory.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Debounce query changes by 300ms
    var localText by remember { mutableStateOf(searchQuery) }
    LaunchedEffect(localText) {
        if (localText != searchQuery) {
            delay(300)
            viewModel.onSearchQueryChanged(localText)
        }
    }

    val categories = listOf(
        "All", "General", "IT", "Finance", "Sales", "Healthcare", "Education",
        "Engineering", "Marketing", "Hospitality", "Logistics", "Admin", "Legal", "Construction"
    )

    val isDarkTheme = com.tpsk.tj.common.ui.theme.LocalIsDarkTheme.current
    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                com.tpsk.tj.common.ui.theme.AppBackgroundDarkTop,
                com.tpsk.tj.common.ui.theme.AppBackgroundDarkBottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.White, com.tpsk.tj.common.ui.theme.AppBackgroundLight)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Color.Transparent
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextField(
                        value = localText,
                        onValueChange = { localText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text("Search jobs by title, company, or keyword...", color = Color.Gray, fontSize = 14.sp)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (localText.isNotEmpty()) {
                                IconButton(onClick = {
                                    localText = ""
                                    viewModel.onSearchQueryChanged("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Category Filter Pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = (selectedCategory == category)
                    AppFilterChip(
                        text = category,
                        isSelected = isSelected,
                        onClick = {
                            viewModel.onSearchCategoryChanged(category)
                        }
                    )
                }
            }

            // Results List or Loading / Empty States
            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchResultsState) {
                    is Resource.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message ?: "Search failed",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.performSearch() }) {
                                Text("Retry")
                            }
                        }
                    }
                    is Resource.Loading -> {
                        if (state.data.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            JobList(
                                jobs = state.data ?: emptyList(),
                                onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                                onSaveClick = { viewModel.toggleSave(it) }
                            )
                        }
                    }
                    is Resource.Success -> {
                        val jobs = state.data.orEmpty()
                        if (jobs.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No matching jobs found",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try adjusting your search query or selecting a different category.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            JobList(
                                jobs = jobs,
                                onJobClick = { navController.navigate(Screen.JobDetail.createRoute(it.id)) },
                                onSaveClick = { viewModel.toggleSave(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
