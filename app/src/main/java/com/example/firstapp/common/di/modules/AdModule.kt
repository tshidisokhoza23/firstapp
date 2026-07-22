package com.example.firstapp.common.di.modules

import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.data.ads.AdMobManagerImpl
import com.example.firstapp.common.data.ads.MaxManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdModule {

    @Provides
    @Singleton
    fun provideAdManager(): AdManager {
        return AdMobManagerImpl()
    }
}
