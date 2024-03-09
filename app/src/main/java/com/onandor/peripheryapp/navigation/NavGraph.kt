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
import com.onandor.peripheryapp.kbm.ui.screens.MainScreen
import com.onandor.peripheryapp.kbm.ui.screens.BtDevicesScreen
import com.onandor.peripheryapp.kbm.ui.screens.BtConnectionTypeSelectScreen
import com.onandor.peripheryapp.kbm.ui.screens.InputScreen
import com.onandor.peripheryapp.kbm.viewmodels.NavigationViewModel
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
    val navManagerState by viewModel.navigationManager.navActions.collectAsState()

    LaunchedEffect(navManagerState) {
        navManagerState?.let {
            try {
                navController.navigate(it.destination, it.navOptions)
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
            composable(NavDestinations.BT_CONNECTION_TYPE_SELECT) {
                BtConnectionTypeSelectScreen()
            }
            composable(NavDestinations.BT_DEVICES) {
                BtDevicesScreen()
            }
            composable(NavDestinations.INPUT) {
                InputScreen()
            }
        }
    }
}