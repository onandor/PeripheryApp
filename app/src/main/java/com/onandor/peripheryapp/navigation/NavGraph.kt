package com.onandor.peripheryapp.navigation

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
import com.onandor.peripheryapp.ui.screens.MainScreen
import com.onandor.peripheryapp.ui.screens.PairBtDeviceScreen
import com.onandor.peripheryapp.ui.screens.BondedBtDevicesScreen
import com.onandor.peripheryapp.viewmodels.NavigationViewModel
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

    Surface {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(NavDestinations.MAIN) {
                MainScreen()
            }
            composable(NavDestinations.BONDED_BT_DEVICES) {
                BondedBtDevicesScreen()
            }
            composable(NavDestinations.PAIR_BT_DEVICE) {
                PairBtDeviceScreen()
            }
        }
    }
}