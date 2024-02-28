package com.onandor.peripheryapp.navigation

import androidx.navigation.NavOptions
import com.onandor.peripheryapp.navigation.Screens.PAIR_BT_DEVICE_SCREEN
import com.onandor.peripheryapp.navigation.Screens.MAIN_SCREEN
import com.onandor.peripheryapp.navigation.Screens.BONDED_BT_DEVICES_SCREEN

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
    // Bluetooth kb&m screens
    const val BONDED_BT_DEVICES_SCREEN = "bondedBtDevicesScreen"
    const val PAIR_BT_DEVICE_SCREEN = "pairBtDeviceScreen"
}

object NavDestinations {
    const val MAIN = MAIN_SCREEN
    // Bluetooth kb&m screens
    const val BONDED_BT_DEVICES = BONDED_BT_DEVICES_SCREEN
    const val PAIR_BT_DEVICE = PAIR_BT_DEVICE_SCREEN
}

interface NavAction {

    val destination: String
    val navOptions: NavOptions
        get() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()
}

object NavActions {
    fun mainNavigation() = object : NavAction {
        override val destination: String = NavDestinations.MAIN
    }

    fun bondedBtDevices() = object : NavAction {
        override val destination: String = NavDestinations.BONDED_BT_DEVICES
    }

    fun pairBtDevice() = object : NavAction {
        override val destination: String = NavDestinations.PAIR_BT_DEVICE
    }
}