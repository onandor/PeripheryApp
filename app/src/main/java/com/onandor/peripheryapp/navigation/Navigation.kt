package com.onandor.peripheryapp.navigation

import androidx.navigation.NavOptions
import com.onandor.peripheryapp.navigation.Screens.NEW_BT_CONNECTION_SCREEN
import com.onandor.peripheryapp.navigation.Screens.MAIN_SCREEN
import com.onandor.peripheryapp.navigation.Screens.PAIRED_BT_DEVICES_SCREEN

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
    // Bluetooth kb&m screens
    const val PAIRED_BT_DEVICES_SCREEN = "pairedBtDevicesScreen"
    const val NEW_BT_CONNECTION_SCREEN = "newBtConnectionScreen"
}

object NavDestinations {
    const val MAIN = MAIN_SCREEN
    // Bluetooth kb&m screens
    const val PAIRED_BT_DEVICES = PAIRED_BT_DEVICES_SCREEN
    const val NEW_BT_CONNECTION = NEW_BT_CONNECTION_SCREEN
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

    fun pairedBtDevices() = object : NavAction {
        override val destination: String = NavDestinations.PAIRED_BT_DEVICES
    }

    fun newBtConnection() = object : NavAction {
        override val destination: String = NavDestinations.NEW_BT_CONNECTION
    }
}