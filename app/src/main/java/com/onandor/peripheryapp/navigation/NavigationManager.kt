package com.onandor.peripheryapp.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Stack

class NavigationManager : INavigationManager {

    private val backStack: Stack<NavAction> = Stack()

    private val _navActions: MutableStateFlow<NavAction?> by lazy {
        MutableStateFlow(null)
    }

    override val navActions: StateFlow<NavAction?> = _navActions.asStateFlow()

    override fun navigateTo(navAction: NavAction?) {
        if (_navActions.value != null) {
            backStack.push(_navActions.value)
        }
        _navActions.update { navAction }
    }

    override fun navigateBack() {
        _navActions.update { backStack.pop() }
    }
}