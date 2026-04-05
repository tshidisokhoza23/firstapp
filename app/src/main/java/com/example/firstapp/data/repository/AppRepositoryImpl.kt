package com.example.firstapp.data.repository

import com.example.firstapp.data.AdvertService
import com.example.firstapp.data.ChannelService
import com.example.firstapp.data.ChatService
import com.example.firstapp.data.JobService
import com.example.firstapp.data.MediaService
import com.example.firstapp.data.MusicService
import com.example.firstapp.data.ShortService
import com.example.firstapp.model.*
import com.example.firstapp.model.Short as AppShort
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AppRepositoryImpl : AppRepository {

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = flow {
        emit(Resource.Success(ChatService.dummyChats))
    }

    override fun getMessages(chatId: Int): Flow<Resource<List<Message>>> = flow {
        val dummyMessages = listOf(
            Message(1, "K", "13:37", false),
            Message(2, "My brada ke batla o nzame ka 2K", "19:20", true),
            Message(3, "Voice call", "19:29", false, MessageType.VOICE_CALL, "2 mins"),
            Message(4, "Voice call", "20:36", false, MessageType.VOICE_CALL, "1 min"),
            Message(5, "Audio message", "20:43", false, MessageType.AUDIO_MESSAGE, "0:13"),
            Message(6, "Oryt my brada", "21:44", true),
            Message(7, "my brada", "19:34", true),
            Message(8, "Voice call", "19:34", true, MessageType.VOICE_CALL, "55 secs"),
            Message(9, "E xap", "19:39", false)
        )
        emit(Resource.Success(dummyMessages))
    }

    override fun getJobs(): Flow<Resource<List<Job>>> = flow {
        emit(Resource.Success(JobService.dummyJobs))
    }

    override fun getAdverts(): Flow<Resource<List<Advert>>> = flow {
        emit(Resource.Success(AdvertService.dummyAdverts))
    }

    override fun getChannels(): Flow<Resource<List<Channel>>> = flow {
        emit(Resource.Success(ChannelService.dummyChannels))
    }

    override fun getShorts(): Flow<Resource<List<AppShort>>> = flow {
        emit(Resource.Success(ShortService.dummyShorts))
    }

    override fun getSongs(): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Success(MusicService.dummySongs))
    }

    override fun getPosts(): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Success(MediaService.dummyPosts))
    }
}
