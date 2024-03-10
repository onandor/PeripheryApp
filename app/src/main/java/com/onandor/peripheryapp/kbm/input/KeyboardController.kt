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
        var scanCode = KeyMapping.keyCodeToScanCode_EnUs[keyCode]
        scanCode?.let { code ->
            sendKeys(modifier, code)
            sendKeys(KeyMapping.ModifierKeys.NONE)
            return if (modifier == KeyMapping.ModifierKeys.NONE) {
                KeyMapping.scanCodeToCharacter_EnUs[scanCode] ?: ""
            } else {
                KeyMapping.shiftScanCodeToCharacter_EnUs[scanCode] ?: ""
            }
        }
        scanCode = KeyMapping.shiftKeyCodeToScanCode_EnUs[keyCode]
        scanCode?.let { code ->
            sendKeys(KeyMapping.ModifierKeys.LEFT_SHIFT, code)
            sendKeys(KeyMapping.ModifierKeys.NONE)
            return KeyMapping.shiftScanCodeToCharacter_EnUs[scanCode] ?: ""
        }
        return ""
    }
}