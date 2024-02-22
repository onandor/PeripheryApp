package com.onandor.peripheryapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.viewmodels.NewBtConnectionViewmodel

@Composable
fun NewBtConnectionScreen(
    viewmodel: NewBtConnectionViewmodel = hiltViewModel()
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Search for devices")
            }
            Button(onClick = { /*TODO*/ }) {
                Text("Turn on discoverability")
            }
        }
    }
}