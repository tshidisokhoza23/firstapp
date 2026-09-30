package com.tpsk.tj.common.data.local

import androidx.room.TypeConverter
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.chat.model.MessageStatus
import com.tpsk.tj.chat.model.MessageType
import com.tpsk.tj.drive.model.DestinationType
import com.tpsk.tj.chat.jobs.model.ApplicationStatus
import com.tpsk.tj.media.model.*
import com.tpsk.tj.chat.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.collections.*

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromConversationType(value: ConversationType): String = value.name

    @TypeConverter
    fun toConversationType(value: String): ConversationType = ConversationType.valueOf(value)

    @TypeConverter
    fun fromMessageType(value: MessageType): String = value.name

    @TypeConverter
    fun toMessageType(value: String): MessageType = MessageType.valueOf(value)

    @TypeConverter
    fun fromPostType(value: PostType): String = value.name

    @TypeConverter
    fun toPostType(value: String): PostType = PostType.valueOf(value)

    @TypeConverter
    fun fromStoryType(value: StoryType): String = value.name

    @TypeConverter
    fun toStoryType(value: String): StoryType = StoryType.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val type = TypeToken.getParameterized(List::class.java, String::class.java).type
            gson.fromJson(value, type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String = gson.toJson(value)

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            // Integer, not Int: R8 keeps List's type bound as a reference type, and primitive int crashes chat open.
            val type = TypeToken.getParameterized(List::class.java, Integer::class.java).type
            gson.fromJson(value, type)
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromPost(value: Post?): String? = gson.toJson(value)

    @TypeConverter
    fun toPost(value: String?): Post? {
        return try {
            gson.fromJson(value, Post::class.java)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromPostList(value: List<Post>?): String = gson.toJson(value)

    @TypeConverter
    fun toPostList(value: String): List<Post>? {
        return try {
            val type = TypeToken.getParameterized(List::class.java, Post::class.java).type
            gson.fromJson(value, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStoryItemList(value: List<StoryItem>?): String = gson.toJson(value)

    @TypeConverter
    fun toStoryItemList(value: String): List<StoryItem>? {
        return try {
            val type = TypeToken.getParameterized(List::class.java, StoryItem::class.java).type
            gson.fromJson(value, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromReactionsMap(value: Map<String, Int>?): String = gson.toJson(value)

    @TypeConverter
    fun toReactionsMap(value: String): Map<String, Int>? {
        return try {
            val type = TypeToken.getParameterized(Map::class.java, String::class.java, java.lang.Integer::class.java).type
            gson.fromJson(value, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromPostPoll(value: PostPoll?): String? = gson.toJson(value)

    @TypeConverter
    fun toPostPoll(value: String?): PostPoll? {
        return try {
            gson.fromJson(value, PostPoll::class.java)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromAdvert(value: Advert?): String? = gson.toJson(value)

    @TypeConverter
    fun toAdvert(value: String?): Advert? {
        return try {
            gson.fromJson(value, Advert::class.java)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromApplicationStatus(value: ApplicationStatus): String = value.name

    @TypeConverter
    fun toApplicationStatus(value: String): ApplicationStatus = ApplicationStatus.valueOf(value)

    @TypeConverter
    fun fromDestinationType(value: DestinationType): String = value.name

    @TypeConverter
    fun toDestinationType(value: String): DestinationType = DestinationType.valueOf(value)

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus): String = value.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}
