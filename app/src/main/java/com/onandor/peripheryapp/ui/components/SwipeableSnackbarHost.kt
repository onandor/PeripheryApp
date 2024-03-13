package com.onandor.peripheryapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    state: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(),
    content: @Composable RowScope.() -> Unit
) {
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.StartToEnd) {
            hostState.currentSnackbarData?.dismiss()
            delay(100)
            state.snapTo(SwipeToDismissBoxValue.StartToEnd)
        }
    }

    SwipeToDismissBox(
        modifier = modifier,
        state = state,
        backgroundContent = {},
        content = content
    )
}