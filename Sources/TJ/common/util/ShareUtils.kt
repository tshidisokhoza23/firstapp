package com.tpsk.tj.common.util

import android.content.Context
import android.content.Intent
import com.tpsk.tj.media.model.Post

object ShareUtils {
    fun sharePost(context: Context, post: Post) {
        val postUrl = "https://firstapp.com/post/${post.id}"
        val sendIntent = Intent()
        sendIntent.setAction(Intent.ACTION_SEND)
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this post on FirstApp: ${post.content}\n\n$postUrl")
        sendIntent.setType("text/plain")

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    fun copyPostLink(context: Context, post: Post) {
        val postUrl = "https://firstapp.com/post/${post.id}"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Post link", postUrl))
        android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
    }
}
