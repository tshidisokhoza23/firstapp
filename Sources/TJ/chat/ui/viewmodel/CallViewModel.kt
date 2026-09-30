package com.tpsk.tj.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tpsk.tj.chat.data.CallTonePlayer
import com.tpsk.tj.chat.data.CallWebSocketClient
import com.tpsk.tj.chat.data.WebRtcCallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    val webRtcCallManager: WebRtcCallManager,
    val callTonePlayer: CallTonePlayer,
    val callWebSocketClient: CallWebSocketClient
) : ViewModel()
