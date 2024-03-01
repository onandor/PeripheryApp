package com.onandor.peripheryapp.kbm.ui.screens

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.kbm.viewmodels.InputViewModel

@Composable
fun InputScreen(
    viewModel: InputViewModel = hiltViewModel()
) {

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TouchSurface()
        }
    }
}

@Composable
private fun TouchSurface() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val posX = change.position.x
                    val poxY = change.position.y
                    Log.d("location", "${change.position}")
                }
            }
    ) {

    }
}