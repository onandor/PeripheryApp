package com.onandor.peripheryapp.di

import android.content.Context
import com.onandor.peripheryapp.kbm.BluetoothController
import com.onandor.peripheryapp.kbm.IBluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothControllerModule {

    @Provides
    @Singleton
    fun provideBluetoothController(@ApplicationContext context: Context): IBluetoothController =
        BluetoothController(context)
}