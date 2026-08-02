package com.example.firstapp.common.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.util.Resource

@Composable
fun <T> DataStateHandler(
    resource: Resource<T>, 
    onLoading: @Composable () -> Unit = { SkeletonLoader() },
    onEmpty: @Composable () -> Unit = { EmptyStateView() },
    onError: @Composable (String?) -> Unit = { ErrorStateView(it) },
    onSuccess: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Loading -> {
            val d = resource.data
            val dList = d as? List<*>
            if (d != null && (dList != null && dList.size > 0)) {
                onSuccess(d)
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else {
                onLoading()
            }
        }
        is Resource.Error -> {
            val d = resource.data
            val dList = d as? List<*>
            if (d != null && (dList != null && dList.size > 0)) {
                onSuccess(d)
                Surface(
                    color = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offline Mode: " + resource.message,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                onError(resource.message)
            }
        }
        is Resource.Success -> {
            val data = resource.data
            val dList = data as? List<*>
            if (data == null || (dList != null && dList.size == 0)) {
                onEmpty()
            } else {
                onSuccess(data)
            }
        }
    }
}

@Composable
fun SkeletonLoader() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        var i = 0
        while (i < 10) {
            SkeletonItem()
            Spacer(modifier = Modifier.height(16.dp))
            i++
        }
    }
}

@Composable
fun ErrorStateView(message: String?) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = message ?: "Connection Error", 
                color = MaterialTheme.colorScheme.onBackground, 
                textAlign = TextAlign.Center
            )
            Button(
                onClick = { },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun SkeletonItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    Row(modifier = Modifier.fillMaxWidth().alpha(alpha)) {
        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.3f)))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.CloudQueue, 
                contentDescription = null, 
                tint = Color.Gray.copy(alpha = 0.5f), 
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Everything is quiet here",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Check back later for updates",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
