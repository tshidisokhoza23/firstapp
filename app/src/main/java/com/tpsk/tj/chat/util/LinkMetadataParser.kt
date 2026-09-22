package com.tpsk.tj.chat.util

import com.tpsk.tj.chat.model.LinkPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object LinkMetadataParser {
    suspend fun fetchMetadata(url: String): LinkPreview? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .timeout(5000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val title = doc.select("meta[property=og:title]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:title]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.title()

            val description = doc.select("meta[property=og:description]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:description]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=description]").attr("content")

            var thumbnailUrl = doc.select("meta[property=og:image]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:image]").attr("content")

            // Ensure absolute URL for thumbnail
            if (thumbnailUrl.isNotBlank() && !thumbnailUrl.startsWith("http")) {
                val baseUri = doc.baseUri().removeSuffix("/")
                thumbnailUrl = if (thumbnailUrl.startsWith("/")) "$baseUri$thumbnailUrl" else "$baseUri/$thumbnailUrl"
            }

            val siteName = doc.select("meta[property=og:site_name]").attr("content").takeIf { it.isNotBlank() }
                ?: doc.select("meta[name=twitter:site]").attr("content")

            LinkPreview(
                url = url,
                title = title.takeIf { it.isNotBlank() },
                description = description.takeIf { it.isNotBlank() },
                thumbnailUrl = thumbnailUrl.takeIf { it.isNotBlank() },
                siteName = siteName.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            android.util.Log.e("LinkMetadataParser", "Error fetching metadata for $url", e)
            null
        }
    }
}
