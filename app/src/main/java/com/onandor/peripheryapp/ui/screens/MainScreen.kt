package com.onandor.peripheryapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MainScreen() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TextButton(onClick = { /*TODO*/ }) {
                Text("Bluetooth keyboard and mouse")
            }
            TextButton(onClick = { /*TODO*/ }) {
                Text("Wifi webcam")
            }
        }
    }
}