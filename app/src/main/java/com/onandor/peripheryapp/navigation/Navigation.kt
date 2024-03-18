package com.onandor.peripheryapp.navigation

import androidx.navigation.NavOptions

private object Screens {
    const val MAIN_SCREEN = "mainScreen"
    object Kbm {
        const val BT_DEVICES_SCREEN = "KBM_btDevices"
        const val INPUT_SCREEN = "KBM_inputScreen"
        const val BT_SETTINGS_SCREEN = "KBM_btSettingsScreen"
    }
    object Webcam {
        const val NEW_CONNECTION_SCREEN = "WEBCAM_newConnectionScreen"
        const val CAMERA_SCREEN = "WEBCAM_cameraScreen"
    }
}

object NavDestinations {
    const val BACK = ""
    const val MAIN = Screens.MAIN_SCREEN
    object Kbm {
        const val BT_DEVICES = Screens.Kbm.BT_DEVICES_SCREEN
        const val INPUT = Screens.Kbm.INPUT_SCREEN
        const val BT_SETTINGS = Screens.Kbm.BT_SETTINGS_SCREEN
    }
    object Webcam {
        const val NEW_CONNECTION = Screens.Webcam.NEW_CONNECTION_SCREEN
        const val CAMERA = Screens.Webcam.CAMERA_SCREEN
    }
}

interface NavAction {

    val destination: String
    val navOptions: NavOptions
        get() = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()
}

object NavActions {
    object Kbm {
        fun btDevices() = object : NavAction {
            override val destination: String = NavDestinations.Kbm.BT_DEVICES
        }

        fun input() = object : NavAction {
            override val destination: String = NavDestinations.Kbm.INPUT
        }

        fun btSettings() = object : NavAction {
            override val destination: String = NavDestinations.Kbm.BT_SETTINGS
        }
    }

    object Webcam {
        fun newConnection() = object : NavAction {
            override val destination: String = NavDestinations.Webcam.NEW_CONNECTION
        }

        fun camera() = object : NavAction {
            override val destination: String = NavDestinations.Webcam.CAMERA
        }
    }

    fun back() = object : NavAction {
        override val destination: String = NavDestinations.BACK
    }

    fun main() = object : NavAction {
        override val destination: String = NavDestinations.MAIN
    }
}