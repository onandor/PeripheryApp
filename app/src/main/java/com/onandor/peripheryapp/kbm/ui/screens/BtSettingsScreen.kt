package com.onandor.peripheryapp.kbm.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.SettingOptions
import com.onandor.peripheryapp.kbm.viewmodels.BtSettingsViewModel
import com.onandor.peripheryapp.ui.components.SettingItem
import com.onandor.peripheryapp.ui.components.SettingsDropdownMenu
import com.onandor.peripheryapp.utils.DropdownItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BtSettingsScreen(
    viewModel: BtSettingsViewModel = hiltViewModel()
) {
    val pollingRateItems = SettingOptions
        .pollingRates
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onPollingRateChanged(option.value) }
            )
        }

    val localeItems = SettingOptions
        .keyboardLocales
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onLocaleChanged(option.value) }
            )
        }

    BackHandler {
        viewModel.onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = viewModel::onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                },
                title = { Text(text = stringResource(id = R.string.bt_settings_title)) }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SettingItem(
                text = stringResource(id = R.string.bt_settings_mouse_polling_rate),
                action = {
                    SettingsDropdownMenu(
                        textToTheLeft = { Text(stringResource(id = uiState.pollingRate.resourceId)) },
                        items = pollingRateItems
                    )
                }
            )
            SettingItem(
                text = stringResource(id = R.string.bt_settings_keyboard_language),
                action = {
                    SettingsDropdownMenu(
                        textToTheLeft = { Text(stringResource(id = uiState.locale.resourceId)) },
                        items = localeItems
                    )
                }
            )
        }
    }
}