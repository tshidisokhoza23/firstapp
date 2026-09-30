package com.tpsk.tj.common.util

sealed class Resource<out T>(val data: T? = null, val message: String? = null) {
    data class Success<out T>(val value: T) : Resource<T>(value)
    data class Error<out T>(val errorMessage: String, val lastData: T? = null) : Resource<T>(lastData, errorMessage)
    data class Loading<out T>(val partialData: T? = null) : Resource<T>(partialData)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Resource<*>) return false
        if (this::class != other::class) return false
        return data == other.data && message == other.message
    }

    override fun hashCode(): Int {
        var result = data?.hashCode() ?: 0
        result = 31 * result + (message?.hashCode() ?: 0)
        return result
    }
}
