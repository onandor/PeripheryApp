package com.onandor.peripheryapp.kbm.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.onandor.peripheryapp.kbm.viewmodels.BtConnectionTypeSelectViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun BtConnectionTypeSelectScreen(
    viewModel: BtConnectionTypeSelectViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    var appSettingsOpen: Boolean = remember { false }
    val _isBluetoothPermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    )
    val isBluetoothPermissionGranted by _isBluetoothPermissionGranted.collectAsState()
    val bluetoothState by viewModel.bluetoothState.collectAsState()

    val _isLocationPermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    )
    val isLocationPermissionGranted by _isLocationPermissionGranted.collectAsState()

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        _isBluetoothPermissionGranted.update {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else {
                true
            }
        }
        _isLocationPermissionGranted.update {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        }
    }

    fun Context.openApplicationSettings() {
        appSettingsOpen = true
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.parse("package:${packageName}")
        })
    }

    BackHandler {
        viewModel.navigateBack()
    }

    LaunchedEffect(lifecycleState) {
        // Check if permission was granted when returning from the application settings
        if (appSettingsOpen && lifecycleState == Lifecycle.State.RESUMED) {
            appSettingsOpen = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                _isBluetoothPermissionGranted.update {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }
    
    LaunchedEffect(isBluetoothPermissionGranted, isLocationPermissionGranted) {
        if (isBluetoothPermissionGranted && isLocationPermissionGranted) {
            viewModel.permissionsGranted()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!isBluetoothPermissionGranted || !isLocationPermissionGranted) {
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
            } else if (bluetoothState != BluetoothAdapter.STATE_ON) {
                Text("Bluetooth is disabled. Enable it to access the functionalities.")
                Button(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                ) {
                    Text(text = "Enable Bluetooth")
                }
            } else {
                Button(onClick = viewModel::navigateToBtDevices) {
                    Text(text = "Connect to a device")
                }
                Button(onClick = { /*TODO*/ }) {
                    Text(text = "Make device discoverable")
                }
            }
        }
    }
}