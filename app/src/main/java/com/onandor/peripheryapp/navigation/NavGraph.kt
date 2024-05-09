package com.onandor.peripheryapp.navigation

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onandor.peripheryapp.ui.MainScreen
import com.onandor.peripheryapp.kbm.ui.screens.BtDevicesScreen
import com.onandor.peripheryapp.kbm.ui.screens.BtSettingsScreen
import com.onandor.peripheryapp.kbm.ui.screens.InputScreen
import com.onandor.peripheryapp.viewmodels.NavigationViewModel
import com.onandor.peripheryapp.webcam.ui.screens.CameraScreen
import com.onandor.peripheryapp.webcam.ui.screens.CameraScreen2
import com.onandor.peripheryapp.webcam.ui.screens.NewConnectionScreen
import com.onandor.peripheryapp.webcam.ui.screens.WebcamSettingsScreen
import java.lang.IllegalArgumentException

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavDestinations.MAIN,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination
    val navManagerState by viewModel.navigationManager.navActions.collectAsState(null)

    LaunchedEffect(navManagerState) {
        navManagerState?.let {
            try {
                if (it.destination == NavDestinations.BACK) {
                    navController.popBackStack()
                } else {
                    navController.navigate(it.destination, it.navOptions)
                }
            } catch (_: IllegalArgumentException) {
                /* Sometimes Live Edit has issues here with the graph, this solves it */
            }
        }
    }

    Surface(modifier = Modifier.navigationBarsPadding()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(NavDestinations.MAIN) {
                MainScreen()
            }
            composable(NavDestinations.Kbm.BT_DEVICES) {
                BtDevicesScreen()
            }
            composable(NavDestinations.Kbm.INPUT) {
                InputScreen()
            }
            composable(NavDestinations.Kbm.BT_SETTINGS) {
                BtSettingsScreen()
            }
            composable(NavDestinations.Webcam.NEW_CONNECTION) {
                NewConnectionScreen()
            }
            composable(NavDestinations.Webcam.CAMERA_2) {
                CameraScreen2()
            }
            composable(NavDestinations.Webcam.SETTINGS) {
                WebcamSettingsScreen()
            }
        }
    }
}