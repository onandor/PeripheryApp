package com.onandor.peripheryapp.kbm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
private fun BluetoothDeviceItem(
    modifier: Modifier,
    name: String,
    description: String = "",
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
    ) {
        Text(text = name)
        if (description.isNotEmpty()) {
            Text(text = description)
        }
    }
}

@Composable
fun FoundBluetoothDeviceItem(
    modifier: Modifier = Modifier,
    name: String,
    bonding: Boolean = false,
    onClick: () -> Unit
) {
    BluetoothDeviceItem(
        modifier = modifier,
        name = name,
        description = if (bonding) "Pairing..." else "",
        onClick = onClick
    )
}

@Composable
fun BondedBluetoothDeviceItem(
    modifier: Modifier = Modifier,
    name: String,
    connecting: Boolean = false,
    connected: Boolean = false,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onUse: () -> Unit
) {
    val description = if (connecting) {
        "Connecting..."
    } else if (connected) {
        "Connected"
    } else {
        ""
    }
    var expanded by remember { mutableStateOf(false) }

    BluetoothDeviceItem(
        modifier = modifier,
        name = name,
        description = description,
        onClick = { expanded = !expanded }
    )
    if (expanded) {
        Row {
            if (!connected) {
                Button(onClick = onConnect) {
                    Text(text = "Connect")
                }
            } else {
                Button(onClick = onUse) {
                    Text(text = "Use")
                }
                Button(onClick = onDisconnect) {
                    Text(text = "Disconnect")
                }
            }
            Button(onClick = onForget) {
                Text(text = "Forget")
            }
        }
    }
}