package com.example.firstapp.common.data.local

import androidx.room.TypeConverter
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.chat.model.MessageStatus
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.drive.model.DestinationType
import com.example.firstapp.chat.jobs.model.ApplicationStatus
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.*
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
    fun toStringList(value: String): List<String>? {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String = gson.toJson(value)

    @TypeConverter
    fun toIntList(value: String): List<Int>? {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
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
            val type = object : TypeToken<List<Post>>() {}.type
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
            val type = object : TypeToken<List<StoryItem>>() {}.type
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
            val type = object : TypeToken<Map<String, Int>>() {}.type
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
