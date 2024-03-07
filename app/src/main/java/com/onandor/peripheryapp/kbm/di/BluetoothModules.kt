package com.onandor.peripheryapp.kbm.di

import android.content.Context
import com.onandor.peripheryapp.kbm.bluetooth.HidDataSender
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceApp
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
import com.onandor.peripheryapp.kbm.input.TouchpadController
import com.onandor.peripheryapp.utils.PermissionChecker
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
    fun provideHidDeviceProfile(
        @ApplicationContext context: Context,
        permissionChecker: PermissionChecker
    ): HidDeviceProfile = HidDeviceProfile(context, permissionChecker)

    @Provides
    @Singleton
    fun provideHidDeviceApp(permissionChecker: PermissionChecker): HidDeviceApp =
        HidDeviceApp(permissionChecker)

    @Provides
    @Singleton
    fun provideHidDataSender(
        @ApplicationContext context: Context,
        hidDeviceApp: HidDeviceApp,
        hidDeviceProfile: HidDeviceProfile
    ): HidDataSender = HidDataSender(context, hidDeviceApp, hidDeviceProfile)

    @Provides
    @Singleton
    fun provideTouchPadController(
        @ApplicationContext context: Context,
        hidDataSender: HidDataSender
    ): TouchpadController = TouchpadController(context, hidDataSender)
}