package com.onandor.peripheryapp.kbm.di

import android.content.Context
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
import com.onandor.peripheryapp.kbm.input.KeyboardController
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
    fun provideBluetoothController(
        @ApplicationContext context: Context,
        hidDeviceProfile: HidDeviceProfile,
        permissionChecker: PermissionChecker
    ): BluetoothController = BluetoothController(context, hidDeviceProfile, permissionChecker)

    @Provides
    @Singleton
    fun provideTouchpadController(
        @ApplicationContext context: Context,
        bluetoothController: BluetoothController
    ): TouchpadController = TouchpadController(context, bluetoothController)

    @Provides
    @Singleton
    fun provideKeyboardController(
        bluetoothController: BluetoothController
    ): KeyboardController = KeyboardController(bluetoothController)
}