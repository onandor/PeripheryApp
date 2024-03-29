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
import androidx.compose.material3.Switch
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
        .pollingRate
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onPollingRateChanged(option.value) }
            )
        }

    val localeItems = SettingOptions
        .keyboardLocale
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onLocaleChanged(option.value) }
            )
        }

    val keyboardReportModeItems = SettingOptions
        .keyboardReportMode
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onKeyboardReportModeChanged(option.value) }
            )
        }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateBack) {
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
            SettingItem(
                text = stringResource(id = R.string.bt_settings_control_volume),
                action = {
                    Switch(
                        checked = uiState.sendVolume,
                        onCheckedChange = { viewModel.onSendVolumeChanged(it) }
                    )
                }
            )
            SettingItem(
                text = stringResource(id = R.string.bt_settings_show_extended_keyboard),
                action = {
                    Switch(
                        checked = uiState.extendedKeyboardShown,
                        onCheckedChange = { viewModel.onExtendedKeyboardChanged(it) }
                    )
                }
            )
            SettingItem(
                text = stringResource(id = R.string.bt_settings_keyboard_report_mode),
                action = {
                    SettingsDropdownMenu(
                        textToTheLeft = {
                            Text(stringResource(id = uiState.keyboardReportMode.resourceId))
                        },
                        items = keyboardReportModeItems
                    )
                }
            )
        }
    }
}