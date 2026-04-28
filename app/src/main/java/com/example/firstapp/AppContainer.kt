package com.example.firstapp

import android.content.Context
import com.example.firstapp.data.local.AppDatabase
import com.example.firstapp.data.repository.AppRepository
import com.example.firstapp.data.repository.AppRepositoryImpl
import com.example.firstapp.data.realtime.WebRtcCallManager
import com.example.firstapp.data.realtime.CallTonePlayer
import com.example.firstapp.vocab.data.repository.UserRepositoryImpl
import com.example.firstapp.vocab.data.repository.MockLessonRepositoryImpl
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.vocab.domain.repository.LessonRepository
import com.example.firstapp.vocab.ads.AdManager

object AppContainer {
    private var _repository: AppRepository? = null
    val repository: AppRepository
        get() = _repository ?: throw IllegalStateException("AppContainer not initialized")

    private var _userRepository: UserRepository? = null
    val userRepository: UserRepository
        get() = _userRepository ?: throw IllegalStateException("UserRepository not initialized")

    private var _lessonRepository: LessonRepository? = null
    val lessonRepository: LessonRepository
        get() = _lessonRepository ?: throw IllegalStateException("LessonRepository not initialized")

    private var _adManager: AdManager? = null
    val adManager: AdManager
        get() = _adManager ?: throw IllegalStateException("AdManager not initialized")

    private var _webRtcCallManager: WebRtcCallManager? = null
    val webRtcCallManager: WebRtcCallManager
        get() = _webRtcCallManager ?: throw IllegalStateException("WebRtcCallManager not initialized")

    private var _callTonePlayer: CallTonePlayer? = null
    val callTonePlayer: CallTonePlayer
        get() = _callTonePlayer ?: throw IllegalStateException("CallTonePlayer not initialized")

    fun initialize(context: Context) {
        val database = AppDatabase.getInstance(context)
        _repository = AppRepositoryImpl(
            database.chatDao(),
            database.songDao(),
            database.jobDao(),
            database.advertDao(),
            database.channelDao(),
            database.postDao(),
            database.storyDao(),
            database.shortDao(),
            database.callLogDao(),
            database.contactDao(),
            database.jobApplicationDao(),
            database.driveDao()
        )
        _userRepository = UserRepositoryImpl(
            database.userDao(),
            database.lessonProgressDao()
        )
        _lessonRepository = MockLessonRepositoryImpl(
            database.lessonProgressDao()
        )
        _adManager = AdManager(context.applicationContext)
        _webRtcCallManager = WebRtcCallManager(context.applicationContext)
        _callTonePlayer = CallTonePlayer()
    }
}
