package com.onandor.peripheryapp.kbm.input

import android.view.KeyEvent
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class MultimediaController @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val settings: Settings
) {

    private var volumeJob: Job? = null
    private var sendVolume: Boolean = false

    fun init() {
        volumeJob = settings
            .observe(BtSettingKeys.SEND_VOLUME_INPUT)
            .onEach { sendVolume = it }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    fun sendMultimedia(keyCode: Int, softwareKey: Boolean) {
        if (keyCode in KeyEvent.KEYCODE_VOLUME_UP..KeyEvent.KEYCODE_VOLUME_DOWN &&
            !(sendVolume || softwareKey)) {
            return
        }
        val scanCode = KeyMapping.multimediaKeyCodeMap[keyCode]
        scanCode?.let {
            bluetoothController.sendMultimedia(scanCode)
            bluetoothController.sendMultimedia(0)
        }
    }

    fun release() {
        volumeJob?.cancel()
        volumeJob = null
    }
}