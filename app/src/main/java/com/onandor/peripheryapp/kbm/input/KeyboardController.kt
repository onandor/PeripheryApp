package com.onandor.peripheryapp.kbm.input

import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import javax.inject.Inject

class KeyboardController @Inject constructor(
    private val bluetoothController: BluetoothController
) {

    private val characterMap = mapOf(
        'a' to 0x04,
        'b' to 0x05,
        'c' to 0x06,
        'd' to 0x07,
        'e' to 0x08,
        'f' to 0x09,
        'g' to 0x0A,
        'h' to 0x0B,
        'i' to 0x0C,
        'j' to 0x0D,
        'k' to 0x0E,
        'l' to 0x0F,
        'm' to 0x10,
        'n' to 0x11,
        'o' to 0x12,
        'p' to 0x13,
        'q' to 0x14,
        'r' to 0x15,
        's' to 0x16,
        't' to 0x17,
        'u' to 0x18,
        'v' to 0x19,
        'w' to 0x1A,
        'x' to 0x1B,
        'y' to 0x1C,
        'z' to 0x1D,
        '1' to 0x1E,
        '2' to 0x1F,
        '3' to 0x20,
        '4' to 0x21,
        '5' to 0x22,
        '6' to 0x23,
        '7' to 0x24,
        '8' to 0x25,
        '9' to 0x26,
        '0' to 0x27,
        ' ' to 0x2C,
        '-' to 0x2D,
        '=' to 0x2E,
        '[' to 0x2F,
        ']' to 0x30,
        '\\' to 0x31,
        ';' to 0x33,
        '\'' to 0x34,
        '`' to 0x35,
        ',' to 0x36,
        '.' to 0x37,
        '/' to 0x38
    )

    private val shiftCharacterMap = mapOf(
        'A' to 0x04,
        'B' to 0x05,
        'C' to 0x06,
        'D' to 0x07,
        'E' to 0x08,
        'F' to 0x09,
        'G' to 0x0A,
        'H' to 0x0B,
        'I' to 0x0C,
        'J' to 0x0D,
        'K' to 0x0E,
        'L' to 0x0F,
        'M' to 0x10,
        'N' to 0x11,
        'O' to 0x12,
        'P' to 0x13,
        'Q' to 0x14,
        'R' to 0x15,
        'S' to 0x16,
        'T' to 0x17,
        'U' to 0x18,
        'V' to 0x19,
        'W' to 0x1A,
        'X' to 0x1B,
        'Y' to 0x1C,
        'Z' to 0x1D,
        '!' to 0x1E,
        '@' to 0x1F,
        '#' to 0x20,
        '$' to 0x21,
        '%' to 0x22,
        '^' to 0x23,
        '&' to 0x24,
        '*' to 0x25,
        '(' to 0x26,
        ',' to 0x27,
        '_' to 0x2D,
        '+' to 0x2E,
        '{' to 0x2F,
        '}' to 0x30,
        '|' to 0x31,
        ':' to 0x33,
        '"' to 0x34,
        '~' to 0x35,
        '<' to 0x36,
        '>' to 0x37,
        '?' to 0x38
    )

    enum class Modifier(val value: Int) {
        NONE(0),
        LEFT_CTRL(1),
        LEFT_SHIFT(2),
        LEFT_ALT(4),
        LEFT_GUI(8),
        RIGHT_CTRL(16),
        RIGHT_SHIFT(32),
        RIGHT_ALT(64),
        RIGHT_GUI(128)
    }

    enum class SpecialKey(val value: Int) {
        RETURN(40),
        ESCAPE(41),
        BACKSPACE(42),
        TAB(43),
        SPACE(44)
    }

    enum class ArrowKey(val value: Int) {
        RIGHT(79),
        LEFT(80),
        DOWN(81),
        UP(82)
    }

    private fun sendKeysDown(
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

    private fun sendKeysUp(modifier: Int) {
        bluetoothController.sendKeyboard(modifier, 0, 0, 0, 0, 0, 0)
    }

    fun sendChar(key: Char) {
        var shift = false
        var keyCode = characterMap[key]
        if (keyCode == null) {
            shift = true
            keyCode = shiftCharacterMap[key]
        }
        val modifier = if (shift) Modifier.LEFT_SHIFT else Modifier.NONE
        keyCode?.let { code ->
            sendKeysDown(modifier.value, code)
            sendKeysUp(Modifier.NONE.value)
        }
    }

    fun sendSpecialKey(key: SpecialKey) {
        sendKeysDown(Modifier.NONE.value, key.value)
        sendKeysUp(Modifier.NONE.value)
    }
}