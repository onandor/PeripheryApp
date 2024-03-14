package com.onandor.peripheryapp.kbm.input

import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class KeyboardController @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val settings: Settings
) {

    private var localeJob: Job? = null
    private var locale = KeyMapping.Locales.EN_US

    fun init() {
        localeJob = settings
            .observe(BtSettingKeys.KEYBOARD_LOCALE, KeyMapping.Locales.EN_US)
            .onEach { locale = it }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    private fun sendKeys(
        modifier: Int,
        key1: Int = 0,
        key2: Int = 0,
        key3: Int = 0,
        key4: Int = 0,
        key5: Int = 0,
        key6: Int = 0
    ) {
        bluetoothController.sendKeyboard(modifier, key1, key2, key3, key4, key5, key6)
    }

    fun sendKey(modifier: Int, keyCode: Int): String {
        val key = if (modifier and KeyMapping.Modifiers.L_SHIFT != 0) {
            KeyMapping.getShiftKeyCodeMap(locale)[keyCode]
        } else {
            KeyMapping.getKeyCodeMap(locale)[keyCode]
        }

        key?.let { (scanCode, keyModifier) ->
            sendKeys(keyModifier or modifier, scanCode)
            sendKeys(KeyMapping.Modifiers.NONE)
        }

        return if (modifier == KeyMapping.Modifiers.NONE) {
            KeyMapping.keyCodeToCharacterMap[keyCode]
        } else {
            KeyMapping.shiftKeyCodeToCharacterMap[keyCode]
        } ?: ""
    }

    fun sendKey(modifier: Int, character: String): String {
        val key = KeyMapping.getSpecialCharacterMap(locale)[character]
        key?.let { (scanCode, keyModifier) ->
            sendKeys(keyModifier or modifier, scanCode)
            sendKeys(KeyMapping.Modifiers.NONE)
            return character
        }
        return ""
    }

    fun sendKeyWithScanCode(modifier: Int, scanCode: Int) {
        sendKeys(modifier, scanCode)
        sendKeys(KeyMapping.Modifiers.NONE)
    }

    fun release() {
        localeJob?.cancel()
        localeJob = null
    }
}