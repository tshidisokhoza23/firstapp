package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Post
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType

object MediaService {
    private fun createDummyReplies(count: Int, depth: Int = 0): List<PostReply> {
        if (depth > 2) return emptyList()
        return List(count) { i ->
            PostReply(
                id = (1000..9999).random(),
                authorName = "Replier ${depth}_$i",
                authorHandle = "@replier_${depth}_$i",
                authorAvatarColor = Color((0..255).random(), (0..255).random(), (0..255).random()),
                content = "This is a reply at depth $depth. Pretty cool thread! #${(1..100).random()}",
                timestamp = "${i + 1}h",
                likes = "${(1..50).random()}",
                nestedReplies = if (depth < 2 && (0..1).random() == 1) createDummyReplies((1..2).random(), depth + 1) else emptyList()
            )
        }
    }

    val dummyPosts = List(30) { index ->
        val type = when (index % 3) {
            0 -> PostType.TEXT_ONLY
            1 -> PostType.IMAGE_WITH_TEXT
            else -> PostType.VIDEO_WITH_TEXT
        }
        Post(
            id = index + 1,
            authorName = if (index == 0) "Tshidiso" else "User ${index + 1}",
            authorHandle = if (index == 0) "@tshidiso_dev" else "@user_${index + 1}",
            authorAvatarColor = Color((0..255).random(), (0..255).random(), (0..255).random()),
            content = "Post number ${index + 1}: Testing ${type.name.replace("_", " ")} with full reply threading support! 🚀 #FirstApp",
            timestamp = "${index + 1}h",
            likes = "${(1..100).random()}K",
            repliesCount = "${(5..50).random()}",
            shares = "${(10..500).random()}",
            views = "${(1..10).random()}M",
            type = type,
            mediaPlaceholderColor = if (type != PostType.TEXT_ONLY) Color((0..255).random(), (0..255).random(), (0..255).random()) else null,
            replies = createDummyReplies((2..5).random())
        )
    }
}
