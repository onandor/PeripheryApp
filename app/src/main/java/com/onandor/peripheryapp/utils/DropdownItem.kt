package com.onandor.peripheryapp.utils

import androidx.compose.runtime.Composable

data class DropdownItem(
    val onClick: () -> Unit,
    val text: @Composable () -> Unit
)