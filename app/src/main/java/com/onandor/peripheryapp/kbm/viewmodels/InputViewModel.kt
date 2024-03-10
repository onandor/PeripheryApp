package com.onandor.peripheryapp.kbm.viewmodels

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.input.KeyboardController
import com.onandor.peripheryapp.kbm.input.MouseButton
import com.onandor.peripheryapp.kbm.input.TouchpadController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class InputUiState(
    val hostName: String = "",
    val isKeyboardShown: Boolean = false,
    val keyboardInputDisplay: String = "",
    val keyboardInputSink: String = "_"
)

@HiltViewModel
class InputViewModel @Inject constructor(
    private val touchpadController: TouchpadController,
    private val keyboardController: KeyboardController,
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val clearTextTimer = object : CountDownTimer(Long.MAX_VALUE, 100) {
        var millisUntilClear = 1000
        var running = false

        fun reset() {
            millisUntilClear = 1000
            if (!running) {
                running = true
                this.start()
            }
        }

        override fun onTick(millisUntilFinished: Long) {
            millisUntilClear -= 100
            if (millisUntilClear == 0) {
                running = false
                _uiState.update { it.copy(keyboardInputDisplay = "") }
                this.cancel()
            }
        }

        override fun onFinish() { }
    }

    private val _uiState = MutableStateFlow(InputUiState())
    val uiState = _uiState.asStateFlow()

    init {
        touchpadController.init()
        _uiState.update { it.copy(hostName = bluetoothController.deviceName) }
    }

    fun buttonDown(button: MouseButton) {
        touchpadController.buttonDown(button)
    }

    fun buttonUp(button: MouseButton) {
        touchpadController.buttonUp(button)
    }

    fun move(x: Float, y: Float) {
        touchpadController.move(x, y)
    }

    fun scroll(wheel: Float) {
        touchpadController.scroll(wheel)
    }

    fun toggleKeyboard() {
        _uiState.update { it.copy(isKeyboardShown = !it.isKeyboardShown) }
    }

    fun keyboardDismissed() {
        _uiState.update { it.copy(isKeyboardShown = false) }
    }

    fun onKeyboardInputChanged(value: String) {
        val backspace = value.length <= uiState.value.keyboardInputSink.length
        if (backspace) {
            keyboardController.sendSpecialKey(KeyboardController.SpecialKey.BACKSPACE)
            _uiState.update {
                it.copy(keyboardInputDisplay = it.keyboardInputDisplay.dropLast(1))
            }
        } else {
            println(value)
            // The last character is always the "_" character
            val lastChar = value[value.length - 2]
            val displayChar: Char
            when (lastChar) {
                '\n' -> {
                    displayChar = '\u23CE'
                    keyboardController.sendSpecialKey(KeyboardController.SpecialKey.RETURN)
                }
                '\t' -> {
                    displayChar = '\u21E5'
                    keyboardController.sendSpecialKey(KeyboardController.SpecialKey.TAB)
                }
                else -> {
                    displayChar = lastChar
                    keyboardController.sendChar(lastChar)
                }
            }
            _uiState.update { it.copy(keyboardInputDisplay = it.keyboardInputDisplay + displayChar) }
        }
        _uiState.update { it.copy(keyboardInputSink = value.ifEmpty { "_" }) }
        clearTextTimer.reset()
    }

    fun onBackspace() {
        keyboardController.sendSpecialKey(KeyboardController.SpecialKey.BACKSPACE)
    }

    override fun onCleared() {
        super.onCleared()
        touchpadController.release()
    }
}