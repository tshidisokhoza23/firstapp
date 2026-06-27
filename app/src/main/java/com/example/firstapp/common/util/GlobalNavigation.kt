package com.example.firstapp.common.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GlobalNavigation {
    private val _navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun navigateTo(route: String) {
        _navigationEvents.tryEmit(route)
    }
}
