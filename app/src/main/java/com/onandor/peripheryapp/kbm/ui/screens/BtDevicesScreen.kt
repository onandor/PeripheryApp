package com.onandor.peripheryapp.kbm.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onandor.peripheryapp.kbm.ui.components.BondedBluetoothDeviceItem
import com.onandor.peripheryapp.kbm.ui.components.FoundBluetoothDeviceItem
import com.onandor.peripheryapp.kbm.viewmodels.BtDevicesViewModel

@SuppressLint("MissingPermission")
@Composable
fun BtDevicesScreen(
    viewModel: BtDevicesViewModel = hiltViewModel()
) {
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateBondedDevices()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.bluetoothState != BluetoothAdapter.STATE_ON) {
                Text("Bluetooth is disabled. Enable it to access the functionalities.")
                Button(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                ) {
                    Text(text = "Enable Bluetooth")
                }
            } else {
                LazyColumn {
                    item {
                        Text(text = "Paired devices:")
                    }
                    uiState.bondedDevices.forEach { device ->
                        item {
                            val connecting =
                                uiState.waitingForDeviceConnecting == device &&
                                    uiState.waitingForDeviceConnecting!!.bondState ==
                                        BluetoothDevice.BOND_BONDED
                            BondedBluetoothDeviceItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                name = device.name ?: device.address,
                                connecting = connecting,
                                connected = uiState.connectedDevice == device,
                                onConnect = { viewModel.connect(device) },
                                onDisconnect = { viewModel.disconnect() },
                                onForget = { viewModel.forget(device) },
                                onUse = { viewModel.navigateToInput() }
                            )
                        }
                    }
                    item {
                        Text(text = "Available devices:")
                    }
                    uiState.foundDevices.forEach { device ->
                        item {
                            val bonding =
                                uiState.waitingForDeviceBonding == device &&
                                    uiState.waitingForDeviceBonding!!.bondState ==
                                        BluetoothDevice.BOND_BONDING
                            FoundBluetoothDeviceItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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