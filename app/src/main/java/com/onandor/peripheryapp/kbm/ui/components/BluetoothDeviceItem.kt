package com.onandor.peripheryapp.kbm.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onandor.peripheryapp.R

@Composable
private fun BluetoothDeviceItem(
    name: String,
    description: String = ""
) {
    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = name,
            fontSize = 18.sp
        )
        if (description.isNotEmpty()) {
            Text(text = description)
        }
    }
}

@Composable
fun FoundBluetoothDeviceItem(
    name: String,
    bonding: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        BluetoothDeviceItem(
            name = name,
            description = if (bonding) stringResource(id = R.string.bt_found_device_pairing) else ""
        )
    }
}

@Composable
fun BondedBluetoothDeviceItem(
    name: String,
    connecting: Boolean = false,
    connected: Boolean = false,
    expanded: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onUse: () -> Unit,
    onClick: () -> Unit
) {
    val description = if (connecting) {
        stringResource(id = R.string.bt_bonded_device_connecting)
    } else if (connected) {
        stringResource(id = R.string.bt_bonded_device_connected)
    } else {
        ""
    }

    val backgroundAlpha by animateFloatAsState(if (expanded) 1f else 0f, label = "")
    val arrowAngle by animateFloatAsState(if (expanded) 180f else 0f, label = "")

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .animateContentSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = backgroundAlpha))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BluetoothDeviceItem(
                name = name,
                description = description
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.rotate(arrowAngle)) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, "")
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
        if (expanded) {
            Row(modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)) {
                if (!connected) {
                    ElevatedButton(onClick = onConnect) {
                        Text(text = stringResource(id = R.string.bt_bonded_device_connect))
                    }
                } else {
                    ElevatedButton(onClick = onUse) {
                        Text(text = stringResource(id = R.string.bt_bonded_device_use))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    ElevatedButton(onClick = onDisconnect) {
                        Text(text = stringResource(id = R.string.bt_bonded_device_disconnect))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                ElevatedButton(onClick = onForget) {
                    Text(text = stringResource(id = R.string.bt_bonded_device_forget))
                }
            }
        }
    }
}