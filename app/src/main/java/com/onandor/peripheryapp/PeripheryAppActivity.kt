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
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavDestinations
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class PeripheryAppActivity : ComponentActivity() {

    @Inject lateinit var settings: Settings
    @Inject lateinit var navManager: INavigationManager
    private var volumeJob: Job? = null
    private var sendVolume: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        volumeJob = settings
            .observe(BtSettingKeys.SEND_VOLUME_INPUT)
            .onEach { sendVolume = it }
            .launchIn(CoroutineScope(Dispatchers.Main))

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
        if (navManager.getCurrentRoute() == NavDestinations.Kbm.INPUT && sendVolume
            && (event?.keyCode == KeyEvent.KEYCODE_VOLUME_UP
            || event?.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}