package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel

private val hashtagRegex = Regex("""#[A-Za-z0-9_]{2,40}""")

@Composable
fun HashtagText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val annotated = remember(text, color) {
        buildAnnotatedString {
            var last = 0
            for (match in hashtagRegex.findAll(text)) {
                append(text.substring(last, match.range.first))
                pushStringAnnotation("hashtag", match.value.removePrefix("#"))
                pushStyle(SpanStyle(color = Color(0xFF1D9BF0), fontWeight = FontWeight.Bold))
                append(match.value)
                pop()
                pop()
                last = match.range.last + 1
            }
            append(text.substring(last))
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = fontSize, lineHeight = lineHeight),
        onClick = { offset ->
            selected = annotated.getStringAnnotations("hashtag", offset, offset).firstOrNull()?.item
        }
    )
    selected?.let { tag ->
        HashtagResultsDialog(tag) { selected = null }
    }
}

@Composable
private fun HashtagResultsDialog(tag: String, onDismiss: () -> Unit) {
    val viewModel: MediaViewModel = hiltViewModel()
    val results by viewModel.searchResults.collectAsState()
    LaunchedEffect(tag) { viewModel.search("#$tag", true) }
    val posts = (results as? Resource.Success)?.data.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("#$tag") },
        text = {
            when {
                results is Resource.Loading -> Text("Searching posts and shorts…")
                posts.isEmpty() -> Text("No posts use #$tag yet.")
                else -> Text(posts.take(8).joinToString("\n") { post ->
                    val who = post.authorName.ifBlank { post.authorHandle }.ifBlank { "Post" }
                    "$who: ${post.content.take(80)}"
                })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
