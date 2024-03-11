package com.onandor.peripheryapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.onandor.peripheryapp.navigation.NavGraph
import com.onandor.peripheryapp.kbm.ui.theme.PeripheryAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PeripheryAppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PeripheryAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_UP || event?.action == KeyEvent.ACTION_MULTIPLE) {
            sendBroadcast(Intent("key_up").apply { putExtra("event", event) })
        }
        if (event?.keyCode == KeyEvent.KEYCODE_ENTER) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}