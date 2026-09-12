package com.example.firstapp.mingle.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.mingle.data.api.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MingleRepository {
    fun getMe(): Flow<Resource<MingleProfileDto>>
    fun updateLocation(lat: Double, lng: Double, radiusKm: Int): Flow<Resource<MingleProfileDto>>
    fun getFeed(lat: Double, lng: Double, radiusKm: Int, offset: Int = 0): Flow<Resource<MingleFeedResponse>>
    fun swipe(targetUserId: String, action: String): Flow<Resource<MingleSwipeResultDto>>
    fun uploadPhoto(file: File): Flow<Resource<MinglePhotoDto>>
    fun deletePhoto(photoId: String): Flow<Resource<Boolean>>
    fun getActivity(): Flow<Resource<List<MingleActivityDto>>>
    fun openChat(userId: String): Flow<Resource<String>>
}
