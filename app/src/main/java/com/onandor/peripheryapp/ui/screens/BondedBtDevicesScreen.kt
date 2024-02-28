package com.onandor.peripheryapp.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onandor.peripheryapp.viewmodels.BondedBtDevicesViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun BondedBtDevicesScreen(
    viewModel: BondedBtDevicesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    var appSettingsOpen: Boolean = remember { false }
    val _canUseBluetooth: MutableStateFlow<Boolean> = MutableStateFlow(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    )
    val canUseBluetooth by _canUseBluetooth.collectAsState()

    val _canUseLocation = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    )
    val canUseLocation by _canUseLocation.collectAsState()

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateBondedDevices()
        }
    }
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        _canUseBluetooth.update {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else {
                true
            }
        }
        _canUseBluetooth.update {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun Context.openApplicationSettings() {
        appSettingsOpen = true
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${packageName}")
        })
    }

    LaunchedEffect(lifecycleState) {
        // Check if permission was granted when returning from the application settings
        if (appSettingsOpen && lifecycleState == Lifecycle.State.RESUMED) {
            appSettingsOpen = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                _canUseBluetooth.update {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }

    LaunchedEffect(canUseBluetooth) {
        if (uiState.isBluetoothEnabled) {
            viewModel.updateBondedDevices()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!canUseBluetooth || !canUseLocation) {
                Text(text = "Bluetooth and/or location permissions are denied. Grant them in " +
                        "order to access the functionalities.")
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    }
                ) {
                    Text(text = "Grant permission")
                }
                Text(text = "If the above button doesn't work, you can grant the permission in the application settings.")
                Button(onClick = { context.openApplicationSettings() }) {
                    Text(text = "Open app settings")
                }
            } else if (!uiState.isBluetoothEnabled) {
                Text("Bluetooth is disabled. Enable it to access the functionalities.")
                Button(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                ) {
                    Text(text = "Enable Bluetooth")
                }
            } else {
                Text("Paired Bluetooth devices:")
                LazyColumn {
                    uiState.bondedDevices.forEach { bondedDevice ->
                        item {
                            Text(
                                text = bondedDevice.name ?: bondedDevice.address,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.connect(bondedDevice) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
                Button(onClick = viewModel::navigateToPairBtDevice) {
                    Text(text = "Pair new device")
                }
            }
        }
    }
}