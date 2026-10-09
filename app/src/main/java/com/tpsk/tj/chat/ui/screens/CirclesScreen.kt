package com.tpsk.tj.chat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.chat.ui.viewmodel.CirclesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CirclesScreen(
    onBack: () -> Unit,
    viewModel: CirclesViewModel = hiltViewModel()
) {
    val circles by viewModel.circles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val message by viewModel.message.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCircleName by remember { mutableStateOf("") }
    var isNewCircleTemporary by remember { mutableStateOf(false) }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
    )

    val filteredCircles = remember(circles, searchQuery) {
        if (searchQuery.isBlank()) circles
        else circles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color(0xFF1A1A1A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Circles",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { showCreateDialog = true }) {
                        Text(
                            text = "+ New",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        floatingActionButton = {
            Button(
                onClick = { showCreateDialog = true },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .background(gradientBrush, RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create New Circle",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search circles...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF242424),
                    unfocusedContainerColor = Color(0xFF242424),
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Text(
                text = "YOUR CIRCLES",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (!message.isNullOrBlank()) {
                Text(message.orEmpty(), color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredCircles, key = { it.id }) { circle ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            // Circle Avatar stack or Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8A2BE2).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = circle.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (circle.isTemporary) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFF9100).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Temporary",
                                                color = Color(0xFFFF9100),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${circle.memberCount} members",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )

                                if (circle.expiresInfo != null) {
                                    Text(
                                        text = circle.expiresInfo,
                                        color = Color(0xFFFF9100),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.deleteCircle(circle.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete circle", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF242424),
            title = {
                Text("Create Circle", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCircleName,
                        onValueChange = { newCircleName = it },
                        placeholder = { Text("Circle name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isNewCircleTemporary,
                            onCheckedChange = { isNewCircleTemporary = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                        )
                        Text("Temporary Circle (Auto-expires)", color = Color.White, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCircleName.isNotBlank()) {
                            viewModel.createCircle(newCircleName, isNewCircleTemporary)
                            newCircleName = ""
                            isNewCircleTemporary = false
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
