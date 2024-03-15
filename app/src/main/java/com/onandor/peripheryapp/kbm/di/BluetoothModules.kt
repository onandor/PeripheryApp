package com.onandor.peripheryapp.kbm.di

import android.content.Context
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
import com.onandor.peripheryapp.kbm.input.KeyboardController
import com.onandor.peripheryapp.kbm.input.MultimediaController
import com.onandor.peripheryapp.kbm.input.TouchpadController
import com.onandor.peripheryapp.utils.PermissionChecker
import com.onandor.peripheryapp.utils.Settings
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
        permissionChecker: PermissionChecker,
        settings: Settings
    ): BluetoothController = BluetoothController(
        context = context,
        hidDeviceProfile = hidDeviceProfile,
        permissionChecker = permissionChecker,
        settings = settings
    )

    @Provides
    @Singleton
    fun provideTouchpadController(
        bluetoothController: BluetoothController,
        settings: Settings
    ): TouchpadController = TouchpadController(bluetoothController, settings)

    @Provides
    @Singleton
    fun provideKeyboardController(
        bluetoothController: BluetoothController,
        settings: Settings
    ): KeyboardController = KeyboardController(bluetoothController, settings)

    @Provides
    @Singleton
    fun provideMultimediaController(
        bluetoothController: BluetoothController,
        settings: Settings
    ): MultimediaController = MultimediaController(bluetoothController, settings)
}