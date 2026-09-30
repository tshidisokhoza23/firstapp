package com.tpsk.tj.stories.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.StoryItem
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.api.ApiViewer
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewersSheet(
    storyItem: StoryItem,
    onDismiss: () -> Unit,
    getViewersFlow: (String) -> Flow<Resource<List<ApiViewer>>>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var viewersState by remember { mutableStateOf<Resource<List<ApiViewer>>>(Resource.Loading()) }

    LaunchedEffect(storyItem.id) {
        getViewersFlow(storyItem.id).collect { res ->
            viewersState = res
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = "Viewers",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val countText = when (val state = viewersState) {
                        is Resource.Success -> "${state.data?.size ?: storyItem.viewsCount} viewers"
                        else -> "${storyItem.viewsCount} viewers"
                    }
                    Text(
                        text = countText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            when (val state = viewersState) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is Resource.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message ?: "Failed to load viewers",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
                is Resource.Success -> {
                    val viewers = state.data.orEmpty()
                    if (viewers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No views yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewers, key = { it.id }) { viewer ->
                                ViewerRow(viewer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerRow(viewer: ApiViewer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!viewer.avatar_url.isNullOrBlank()) {
                AsyncImage(
                    model = viewer.avatar_url,
                    contentDescription = viewer.display_name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = viewer.display_name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewer.display_name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!viewer.viewed_at.isNullOrBlank()) {
                Text(
                    text = viewer.viewed_at,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
