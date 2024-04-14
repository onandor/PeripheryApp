package com.onandor.peripheryapp.webcam.di

import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamerModule {

    @Singleton
    @Provides
    fun provideStreamer(): Streamer = Streamer()
}