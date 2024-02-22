package com.onandor.peripheryapp.navigation

import androidx.navigation.NavOptions
import com.onandor.peripheryapp.navigation.Screens.MAIN_SCREEN

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
}

object NavDestinations {
    const val MAIN = MAIN_SCREEN
}

interface NavAction {

    val destination: String
    val navOptions: NavOptions
        get() = NavOptions.Builder()
            .setPopUpTo(0, true)
            .setLaunchSingleTop(true)
            .build()
}

object NavActions {
    fun mainNavigation() = object : NavAction {
        override val destination: String = NavDestinations.MAIN
    }
}