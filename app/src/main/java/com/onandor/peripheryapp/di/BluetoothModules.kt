package com.onandor.peripheryapp.di

import android.content.Context
import com.onandor.peripheryapp.kbm.BluetoothController
import com.onandor.peripheryapp.kbm.HidDataSender
import com.onandor.peripheryapp.kbm.HidDeviceApp
import com.onandor.peripheryapp.kbm.HidDeviceProfile
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
    fun provideHidDeviceProfile(@ApplicationContext context: Context): HidDeviceProfile =
        HidDeviceProfile(context)

    @Provides
    @Singleton
    fun provideHidDeviceApp(): HidDeviceApp = HidDeviceApp()

    @Provides
    @Singleton
    fun provideHidDataSender(
        hidDeviceApp: HidDeviceApp,
        hidDeviceProfile: HidDeviceProfile
    ): HidDataSender = HidDataSender(hidDeviceApp, hidDeviceProfile)

    @Provides
    @Singleton
    fun provideBluetoothController(
        @ApplicationContext context: Context,
        hidDataSender: HidDataSender
    ): IBluetoothController = BluetoothController(context, hidDataSender)
}