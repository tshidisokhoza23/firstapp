package com.example.firstapp.common.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StoryMediaContainer(
    mediaUri: Uri?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (mediaUri != null) {
            // Dynamic Ambient Background: Blurred duplicate to fill 9:16 screen
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 35.dp)
                    .alpha(0.6f),
                contentScale = ContentScale.Crop
            )

            // Main Foreground Media (Respects intrinsic aspect ratio with letterboxing)
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.Fit
            )
        }

        content()
    }
}
