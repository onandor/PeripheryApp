package com.onandor.peripheryapp.kbm.input

import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import javax.inject.Inject

class KeyboardController @Inject constructor(
    private val bluetoothController: BluetoothController
) {

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
        val key = if (modifier == KeyMapping.Modifiers.NONE) {
            KeyMapping.keyCodeMap_HuHu[keyCode]
        } else {
            KeyMapping.shiftKeyCodeMap_HuHu[keyCode]
        }

        key?.let { (scanCode, modifier) ->
            sendKeys(modifier, scanCode)
            sendKeys(KeyMapping.Modifiers.NONE)
        }

        return if (modifier == KeyMapping.Modifiers.NONE) {
            KeyMapping.keyCodeToCharacterMap[keyCode]
        } else {
            KeyMapping.shiftKeyCodeToCharacterMap[keyCode]
        } ?: ""
    }
}