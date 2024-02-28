package com.onandor.peripheryapp.navigation

import androidx.navigation.NavOptions
import com.onandor.peripheryapp.navigation.Screens.BT_DEVICES_SCREEN
import com.onandor.peripheryapp.navigation.Screens.MAIN_SCREEN
import com.onandor.peripheryapp.navigation.Screens.BT_CONNECTION_TYPE_SELECT_SCREEN

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
    // Bluetooth kb&m screens
    const val BT_DEVICES_SCREEN = "btDevices"
    const val BT_CONNECTION_TYPE_SELECT_SCREEN = "btConnectionTypeSelectScreen"
}

object NavDestinations {
    const val MAIN = MAIN_SCREEN
    // Bluetooth kb&m screens
    const val BT_DEVICES = BT_DEVICES_SCREEN
    const val BT_CONNECTION_TYPE_SELECT = BT_CONNECTION_TYPE_SELECT_SCREEN
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

    fun btDevices() = object : NavAction {
        override val destination: String = NavDestinations.BT_DEVICES
    }

    fun btConnectionTypeSelect() = object : NavAction {
        override val destination: String = NavDestinations.BT_CONNECTION_TYPE_SELECT
    }
}