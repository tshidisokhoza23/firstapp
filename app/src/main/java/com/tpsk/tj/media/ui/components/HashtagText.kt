package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.tpsk.tj.common.util.GlobalNavigation
import com.tpsk.tj.ui.navigation.Screen

private val hashtagRegex = Regex("""#[A-Za-z0-9_]{2,40}""")

@Composable
fun HashtagText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: TextUnit = TextUnit.Unspecified,
    onHashtagClick: ((String) -> Unit)? = null
) {
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
            annotated.getStringAnnotations("hashtag", offset, offset).firstOrNull()?.item?.let { tag ->
                if (onHashtagClick != null) {
                    onHashtagClick(tag)
                } else {
                    GlobalNavigation.navigateTo(Screen.ContestFeed.createRoute("#$tag"))
                }
            }
        }
    )
}
