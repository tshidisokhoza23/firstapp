package com.tpsk.tj.common.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.compose.runtime.mutableStateOf

object GlobalNavigation {
    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    val isBottomBarVisible = mutableStateOf(false)

    fun navigateTo(route: String) {
        _navigationEvents.tryEmit(route)
    }
}
