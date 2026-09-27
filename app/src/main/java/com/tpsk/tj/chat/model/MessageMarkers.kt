package com.tpsk.tj.chat.model

/**
 * Fields the server may drop (story reply, shared contact) are also written
 * into the message body so the message list can still render them.
 */
object MessageMarkers {
    private const val MARK = "\u2060"
    private const val SEP = "\u001f"
    private val markerRegex = Regex("\u2060\\[(?:storyReply|contact):[^\\]]*\\]")

    data class StoryReply(
        val storyId: String,
        val userId: String,
        val author: String,
        val summary: String,
        val mediaUrl: String
    )

    data class SharedContact(val name: String, val phone: String)

    fun storyReplyId(storyId: String, userId: String): String {
        val id = storyId.removePrefix("story_").substringBefore("|")
        val user = userId.removePrefix("user_")
        return if (user.isBlank()) "story_$id" else "story_$id|$user"
    }

    fun parseStoryReplyId(replyToId: String?): StoryReply? {
        if (replyToId.isNullOrBlank() || !replyToId.startsWith("story_")) return null
        val rest = replyToId.removePrefix("story_")
        val parts = rest.split("|", limit = 2)
        val storyId = parts.firstOrNull()?.trim().orEmpty()
        if (storyId.isBlank()) return null
        return StoryReply(
            storyId = storyId,
            userId = parts.getOrNull(1)?.trim().orEmpty(),
            author = "",
            summary = "",
            mediaUrl = ""
        )
    }

    fun storyWire(text: String, replyToId: String?, replyToText: String?, author: String?): String {
        val ref = parseStoryReplyId(replyToId) ?: return text
        val summary = strip(replyToText.orEmpty())
        val media = Regex("\\[mediaUrl:(.*?)]").find(replyToText.orEmpty())?.groupValues?.getOrNull(1)?.trim().orEmpty()
        val payload = listOf(ref.storyId, ref.userId, author.orEmpty(), summary, media).joinToString(SEP)
        return strip(text) + MARK + "[storyReply:$payload]"
    }

    fun contactWire(name: String?, phone: String?, fallback: String = "Shared Contact"): String {
        val safeName = name?.replace(SEP, " ")?.trim().orEmpty()
        val safePhone = phone?.replace(SEP, " ")?.trim().orEmpty()
        if (safeName.isBlank() && safePhone.isBlank()) return fallback
        return "Shared Contact" + MARK + "[contact:$safeName$SEP$safePhone]"
    }

    fun strip(content: String?): String = content.orEmpty().replace(markerRegex, "").trim()

    fun parseStory(content: String?): StoryReply? {
        val raw = Regex("\u2060\\[storyReply:([^\\]]*)]").find(content.orEmpty())?.groupValues?.getOrNull(1)
            ?: return null
        val parts = raw.split(SEP)
        val storyId = parts.getOrNull(0)?.trim().orEmpty()
        if (storyId.isBlank()) return null
        return StoryReply(
            storyId = storyId,
            userId = parts.getOrNull(1)?.trim().orEmpty(),
            author = parts.getOrNull(2)?.trim().orEmpty(),
            summary = parts.getOrNull(3)?.trim().orEmpty(),
            mediaUrl = parts.getOrNull(4)?.trim().orEmpty()
        )
    }

    fun parseContact(content: String?): SharedContact? {
        val raw = Regex("\u2060\\[contact:([^\\]]*)]").find(content.orEmpty())?.groupValues?.getOrNull(1)
            ?: return null
        val parts = raw.split(SEP)
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val phone = parts.getOrNull(1)?.trim().orEmpty()
        if (name.isBlank() && phone.isBlank()) return null
        return SharedContact(name, phone)
    }
}
