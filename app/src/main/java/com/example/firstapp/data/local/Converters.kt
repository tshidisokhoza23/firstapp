package com.example.firstapp.data.local

import androidx.room.TypeConverter
import com.example.firstapp.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

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
    fun fromPostReplyList(value: List<PostReply>?): String = gson.toJson(value)

    @TypeConverter
    fun toPostReplyList(value: String): List<PostReply>? {
        val type = object : TypeToken<List<PostReply>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromStoryItemList(value: List<StoryItem>?): String = gson.toJson(value)

    @TypeConverter
    fun toStoryItemList(value: String): List<StoryItem>? {
        val type = object : TypeToken<List<StoryItem>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromReactionsMap(value: Map<String, Int>?): String = gson.toJson(value)

    @TypeConverter
    fun toReactionsMap(value: String): Map<String, Int>? {
        val type = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromApplicationStatus(value: ApplicationStatus): String = value.name

    @TypeConverter
    fun toApplicationStatus(value: String): ApplicationStatus = ApplicationStatus.valueOf(value)

    @TypeConverter
    fun fromDestinationType(value: DestinationType): String = value.name

    @TypeConverter
    fun toDestinationType(value: String): DestinationType = DestinationType.valueOf(value)
}
