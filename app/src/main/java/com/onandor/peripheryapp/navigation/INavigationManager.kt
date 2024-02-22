package com.onandor.peripheryapp.navigation

import kotlinx.coroutines.flow.StateFlow

interface INavigationManager {

    val navActions: StateFlow<NavAction?>
    fun navigateTo(navAction: NavAction?)
    fun navigateBack()
}