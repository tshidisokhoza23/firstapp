package com.example.firstapp.common.util

import android.content.Context
import android.content.Intent
import com.example.firstapp.media.model.Post

object ShareUtils {
    fun sharePost(context: Context, post: Post) {
        val sendIntent = Intent()
        sendIntent.setAction(Intent.ACTION_SEND)
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this post on SuperApp: ${post.content}\n\nShared from SuperApp")
        sendIntent.setType("text/plain")

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}
