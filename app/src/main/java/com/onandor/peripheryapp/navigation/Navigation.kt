package com.onandor.peripheryapp.navigation

import androidx.navigation.NavOptions
import com.onandor.peripheryapp.navigation.Screens.BT_DEVICES_SCREEN
import com.onandor.peripheryapp.navigation.Screens.MAIN_SCREEN
import com.onandor.peripheryapp.navigation.Screens.BT_SETTINGS_SCREEN
import com.onandor.peripheryapp.navigation.Screens.INPUT_SCREEN

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
    // Bluetooth kb&m screens
    const val BT_DEVICES_SCREEN = "btDevices"
    const val INPUT_SCREEN = "inputScreen"
    const val BT_SETTINGS_SCREEN = "btSettingsScreen"
}

object NavDestinations {
    const val BACK = ""
    const val MAIN = MAIN_SCREEN
    // Bluetooth kb&m screens
    const val BT_DEVICES = BT_DEVICES_SCREEN
    const val INPUT = INPUT_SCREEN
    const val BT_SETTINGS = BT_SETTINGS_SCREEN
}

interface NavAction {

    val destination: String
    val navOptions: NavOptions
        get() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()
}

object NavActions {
    fun back() = object : NavAction {
        override val destination: String = NavDestinations.BACK
    }

    fun main() = object : NavAction {
        override val destination: String = NavDestinations.MAIN
    }

    fun btDevices() = object : NavAction {
        override val destination: String = NavDestinations.BT_DEVICES
    }

    fun input() = object : NavAction {
        override val destination: String = NavDestinations.INPUT
    }

    fun btSettings() = object : NavAction {
        override val destination: String = NavDestinations.BT_SETTINGS
    }
}