package com.onandor.peripheryapp.kbm.ui.components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.onandor.peripheryapp.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun PermissionRequest(
    bluetoothState: Int,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val _isBluetoothPermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    )
    val isBluetoothPermissionGranted by _isBluetoothPermissionGranted.collectAsState()

    val _isLocationPermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    )
    val isLocationPermissionGranted by _isLocationPermissionGranted.collectAsState()

    LaunchedEffect(isBluetoothPermissionGranted, isLocationPermissionGranted) {
        if (isBluetoothPermissionGranted && isLocationPermissionGranted) {
            onPermissionsGranted()
        }
    }

    if (!isBluetoothPermissionGranted || !isLocationPermissionGranted) {
        PermissionsMissing(
            onBluetoothPermissionChanged = { granted ->
                _isBluetoothPermissionGranted.update { granted }
            },
            onLocationPermissionChanged = { granted ->
                _isLocationPermissionGranted.update { granted }
            }
        )
    } else if (bluetoothState != BluetoothAdapter.STATE_ON) {
        BluetoothOff()
    }
}

@Composable
private fun PermissionsMissing(
    onBluetoothPermissionChanged: (Boolean) -> Unit,
    onLocationPermissionChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val packageName = context.packageName

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bluetoothPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }
        val locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        onBluetoothPermissionChanged(bluetoothPermissionGranted)
        onLocationPermissionChanged(locationPermissionGranted)
    }

    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            onBluetoothPermissionChanged(permissionGranted)
        } else {
            onBluetoothPermissionChanged(true)
        }
        val locationPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        onLocationPermissionChanged(locationPermissionGranted)
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Icon(
                    modifier = Modifier.size(120.dp),
                    painter = painterResource(id = R.drawable.ic_bluetooth_disabled),
                    contentDescription = ""
                )
                Icon(
                    modifier = Modifier.size(120.dp),
                    painter = painterResource(id = R.drawable.ic_location_off),
                    contentDescription = ""
                )
            }
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(R.string.permission_request_permissions_denied),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Row {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.BLUETOOTH_ADVERTISE
                                )
                            )
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.permission_request_grant_permissions))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(
                    onClick = {
                        appSettingsLauncher.launch(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.parse("package:${packageName}")
                        })
                    }
                ) {
                    Text(text = stringResource(R.string.permission_request_app_settings))
                }
            }
            Text(
                modifier = Modifier.padding(top = 20.dp, start = 30.dp, end = 30.dp),
                text = stringResource(R.string.permission_request_permissions_denied_settings),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BluetoothOff() {
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    Surface {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(180.dp),
                painter = painterResource(R.drawable.ic_bluetooth_disabled),
                contentDescription = ""
            )
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(id = R.string.permission_request_bt_turned_off),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            ) {
                Text(text = stringResource(id = R.string.permission_request_turn_on))
            }
        }
    }
}
