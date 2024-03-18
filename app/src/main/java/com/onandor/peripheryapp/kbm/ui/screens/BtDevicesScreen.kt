package com.onandor.peripheryapp.kbm.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.ui.components.BondedBluetoothDeviceItem
import com.onandor.peripheryapp.kbm.ui.components.FoundBluetoothDeviceItem
import com.onandor.peripheryapp.kbm.ui.components.PermissionRequest
import com.onandor.peripheryapp.kbm.viewmodels.BtDevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun BtDevicesScreen(
    viewModel: BtDevicesViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.bt_devices_title)) },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.bluetoothState != BluetoothAdapter.STATE_ON ||
                !uiState.arePermissionsGranted) {
                PermissionRequest(
                    appSettingsOpen = uiState.isAppSettingsOpen,
                    bluetoothState = uiState.bluetoothState,
                    onPermissionsGranted = viewModel::onPermissionsGranted,
                    onAppSettingsOpenChanged = viewModel::onAppSettingsOpenChanged
                )
            } else {
                LazyColumn {
                    item {
                        EnableDiscoverability(remainingDiscoverable = uiState.remainingDiscoverable)
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(top = 10.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(id = R.string.bt_devices_paired_devices),
                            fontSize = 18.sp
                        )
                    }
                    uiState.bondedDevices.forEach { device ->
                        item {
                            val connecting =
                                uiState.waitingForDeviceConnecting == device &&
                                    uiState.waitingForDeviceConnecting!!.bondState ==
                                        BluetoothDevice.BOND_BONDED
                            BondedBluetoothDeviceItem(
                                name = device.name ?: device.address,
                                connecting = connecting,
                                connected = uiState.connectedDevice == device,
                                expanded = uiState.expandedBondedDevice == device,
                                onConnect = { viewModel.connect(device) },
                                onDisconnect = { viewModel.disconnect() },
                                onForget = { viewModel.forget(device) },
                                onUse = { viewModel.navigateToInput() },
                                onClick = { viewModel.onBondedDeviceClick(device) }
                            )
                        }
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier
                                .padding(top = 10.dp, bottom = 10.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = stringResource(id = R.string.bt_devices_available_devices),
                            fontSize = 18.sp
                        )
                    }
                    uiState.foundDevices.forEach { device ->
                        item {
                            val bonding =
                                uiState.waitingForDeviceBonding == device &&
                                    uiState.waitingForDeviceBonding!!.bondState ==
                                        BluetoothDevice.BOND_BONDING
                            FoundBluetoothDeviceItem(
                                name = device.name ?: device.address,
                                bonding = bonding,
                                onClick = { viewModel.pair(device) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnableDiscoverability(remainingDiscoverable: Int) {
    val discoverabilityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = remainingDiscoverable <= 0) {
                val intent =
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                discoverabilityLauncher.launch(intent)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val text = if (remainingDiscoverable <= 0) {
            stringResource(R.string.bt_devices_make_device_discoverable)
        } else {
            buildString {
                append(stringResource(R.string.bt_devices_device_is_discoverable_for))
                append(" ")
                append(remainingDiscoverable)
                append(" ")
                append(stringResource(id = R.string.second_short))
            }
        }
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = text,
            fontSize = 18.sp
        )
        Checkbox(
            enabled = false,
            checked = remainingDiscoverable > 0,
            onCheckedChange = {}
        )
    }
}