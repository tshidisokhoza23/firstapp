package com.tpsk.tj.common.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.common.data.network.NetworkEnvironmentManager
import com.tpsk.tj.common.data.network.NetworkMode
import com.tpsk.tj.common.ui.theme.AppSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDiagnosticsSheet(
    envManager: NetworkEnvironmentManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    var activeLatency by remember { mutableStateOf<String?>(null) }
    var passiveLatency by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSecondary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // FIX: Adds padding so the sheet is above the bottom navigation bar
                .padding(24.dp)
        ) {
            Text("Developer Diagnostics", color = Color.White, fontSize = 20.sp, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Server Mode", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                var currentMode by remember { mutableStateOf(envManager.mode) }
                RadioButton(
                    selected = currentMode == NetworkMode.FORCE_ACTIVE,
                    onClick = { 
                        envManager.mode = NetworkMode.FORCE_ACTIVE
                        currentMode = NetworkMode.FORCE_ACTIVE
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Active Server", color = Color.White)
                Spacer(modifier = Modifier.width(24.dp))
                RadioButton(
                    selected = currentMode == NetworkMode.FORCE_PASSIVE,
                    onClick = { 
                        envManager.mode = NetworkMode.FORCE_PASSIVE
                        currentMode = NetworkMode.FORCE_PASSIVE
                    },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Text("Passive Server", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    isPinging = true
                    scope.launch {
                        activeLatency = pingServer(envManager.activeUrl)
                        passiveLatency = pingServer(envManager.passiveUrl)
                        isPinging = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPinging
            ) {
                if (isPinging) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Ping Servers")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Active Status:", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    val color = if (activeLatency?.contains("200") == true) Color.Green else Color.White
                    Text(activeLatency ?: "N/A", color = color)
                }
                Column {
                    Text("Passive Status:", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    val color = if (passiveLatency?.contains("200") == true) Color.Green else Color.White
                    Text(passiveLatency ?: "N/A", color = color)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private suspend fun pingServer(url: String): String {
    val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
        
    val sanitizedUrl = if (url.length > 0 && url[url.length - 1] == '/') {
        url.substring(0, url.length - 1)
    } else {
        url
    }

    val request = Request.Builder()
        .url("$sanitizedUrl/healthz")
        .build()
        
    return try {
        val start = System.currentTimeMillis()
        val response = withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
        val duration = System.currentTimeMillis() - start
        if (response.isSuccessful) {
            "${duration}ms [200 OK]"
        } else {
            "Error [${response.code}]"
        }
    } catch (e: Exception) {
        "Unreachable [${e.message}]"
    }
}
