package com.onandor.peripheryapp.kbm.di

import android.content.Context
import com.onandor.peripheryapp.kbm.bluetooth.HidDataSender
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceApp
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
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
    fun provideHidDeviceProfile(@ApplicationContext context: Context): HidDeviceProfile =
        HidDeviceProfile(context)

    @Provides
    @Singleton
    fun provideHidDeviceApp(@ApplicationContext context: Context): HidDeviceApp =
        HidDeviceApp(context)

    @Provides
    @Singleton
    fun provideHidDataSender(
        hidDeviceApp: HidDeviceApp,
        hidDeviceProfile: HidDeviceProfile
    ): HidDataSender = HidDataSender(hidDeviceApp, hidDeviceProfile)
}