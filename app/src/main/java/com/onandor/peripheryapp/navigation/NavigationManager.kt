package com.onandor.peripheryapp.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack

class NavigationManager : INavigationManager {

    private val backStack: Stack<NavAction> = Stack()

    private val _navActions: MutableSharedFlow<NavAction?> by lazy { MutableSharedFlow() }

    override val navActions: SharedFlow<NavAction?> = _navActions.asSharedFlow()

    private var currentRoute: String = ""

    init {
        backStack.push(NavActions.main())
        currentRoute = NavDestinations.MAIN
    }

    override fun navigateTo(navAction: NavAction?) {
        navAction?.let {
            if (navAction.navOptions.popUpToId == 0) {
                backStack.clear()
            }
            backStack.push(navAction)
            currentRoute = navAction.destination
            CoroutineScope(Dispatchers.Main).launch {
                _navActions.emit(navAction)
            }
        }
    }

    override fun navigateBack() {
        if (backStack.isNotEmpty()) {
            backStack.pop()
            currentRoute = backStack.peek().destination
            CoroutineScope(Dispatchers.Main).launch {
                _navActions.emit(NavActions.back())
            }
        }
    }

    override fun getCurrentRoute(): String {
        return currentRoute
    }
}