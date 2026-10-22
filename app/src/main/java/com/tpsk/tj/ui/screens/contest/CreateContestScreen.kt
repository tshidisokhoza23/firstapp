package com.tpsk.tj.ui.screens.contest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContestScreen(
    onNavigateBack: () -> Unit,
    onContestCreated: (String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtag by remember { mutableStateOf("") }
    var participantLimit by remember { mutableStateOf("20") }
    var generatedCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSubmitted by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val contestViewModel: ContestViewModel = hiltViewModel()

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Host a Pageant / Contest", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isSubmitted) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Contest Title (e.g. Miss Thembisa 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = surfaceColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                // Hashtag Field
                OutlinedTextField(
                    value = hashtag,
                    onValueChange = {
                        val cleaned = it.replace(" ", "").lowercase()
                        hashtag = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
                    },
                    label = { Text("Custom Hashtag (e.g. #MissThembisa)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = surfaceColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Contest Bio & Rules") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = surfaceColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                // Participant Limit
                OutlinedTextField(
                    value = participantLimit,
                    onValueChange = { participantLimit = it.filter { char -> char.isDigit() } },
                    label = { Text("Contestant Limit (Default: 20)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = surfaceColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Launch Button
                Button(
                    onClick = {
                        if (title.isBlank() || hashtag.isBlank() || isSaving) {
                            Toast.makeText(context, "Please enter a title and hashtag", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSaving = true
                        val limit = participantLimit.toIntOrNull() ?: 20
                        contestViewModel.createContest(
                            title = title,
                            description = description,
                            hashtag = hashtag,
                            limit = limit,
                            onSuccess = { created ->
                                generatedCodes = created.codes.orEmpty()
                                hashtag = "#${created.hashtag.removePrefix("#")}"
                                isSaving = false
                                isSubmitted = true
                            },
                            onError = { message ->
                                isSaving = false
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🚀 Create and Launch Contest", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            } else {
                // Generated Manager Access Codes View
                Text("✅ Contest Created Successfully!", color = GoldYellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Share these Manager Access Codes with contestant managers. Each code grants 1 verified entry to $hashtag.", color = secondaryTextColor, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(8.dp))

                generatedCodes.forEach { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(code, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Manager Code", code))
                            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = accentColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onContestCreated(hashtag) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to Contest Feed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}
