package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.firstapp.chat.data.CallTonePlayer
import com.example.firstapp.chat.data.CallWebSocketClient
import com.example.firstapp.chat.data.WebRtcCallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    val webRtcCallManager: WebRtcCallManager,
    val callTonePlayer: CallTonePlayer,
    val callWebSocketClient: CallWebSocketClient
) : ViewModel()
