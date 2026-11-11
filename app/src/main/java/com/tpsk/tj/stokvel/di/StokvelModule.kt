package com.tpsk.tj.stokvel.di

import com.tpsk.tj.stokvel.data.remote.StokvelApi
import com.tpsk.tj.stokvel.data.repository.StokvelRepositoryImpl
import com.tpsk.tj.stokvel.domain.repository.StokvelRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StokvelModule {

    @Provides
    @Singleton
    fun provideStokvelApi(retrofit: Retrofit): StokvelApi {
        return retrofit.create(StokvelApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StokvelRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindStokvelRepository(
        stokvelRepositoryImpl: StokvelRepositoryImpl
    ): StokvelRepository
}
