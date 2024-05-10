package com.onandor.peripheryapp.navigation

import kotlinx.coroutines.flow.SharedFlow

interface INavigationManager {

    val navActions: SharedFlow<NavAction?>
    fun navigateTo(navAction: NavAction?)
    fun navigateBack()
    fun getCurrentRoute(): String
    fun getCurrentNavAction(): NavAction?
}