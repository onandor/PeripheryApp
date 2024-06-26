package com.onandor.peripheryapp.webcam.di

import android.content.Context
import com.onandor.peripheryapp.webcam.video.CameraController
import com.onandor.peripheryapp.webcam.network.TcpServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamerModule {

    @Singleton
    @Provides
    fun provideTcpServer(): TcpServer = TcpServer()
}

@Module
@InstallIn(SingletonComponent::class)
object CameraControllerModule {

    @Singleton
    @Provides
    fun provideCameraController(@ApplicationContext context: Context) = CameraController(context)
}