package com.onandor.peripheryapp.kbm.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.input.KeyMapping
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
    val keyboardInput: String = "",
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
                _uiState.update { it.copy(keyboardInput = "") }
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

    fun keyboardDismissed() {
        _uiState.update { it.copy(isKeyboardShown = false) }
    }

    fun onKeyPressed(event: KeyEvent) {
        if (event.keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            specialCharacterPressed(event)
            return
        }
        val modifier = if (event.isShiftPressed) {
            KeyMapping.Modifiers.L_SHIFT
        } else {
            KeyMapping.Modifiers.NONE
        }
        val character = keyboardController.sendKey(modifier, event.keyCode)
        if (character == KeyMapping.BACKSPACE) {
            _uiState.update { it.copy(keyboardInput = it.keyboardInput.dropLast(1)) }
        } else {
            _uiState.update { it.copy(keyboardInput = it.keyboardInput + character) }
        }
        clearTextTimer.reset()
    }

    private fun specialCharacterPressed(event: KeyEvent) {
        if (event.characters == null) {
            return
        }
        val character = keyboardController.sendKey(event.characters)
        if (character.isNotEmpty()) {
            _uiState.update { it.copy(keyboardInput = it.keyboardInput + character) }
            clearTextTimer.reset()
        }
    }

    fun toggleKeyboard(context: Context) {
        val view = (context as Activity).findViewById<View>(android.R.id.content)
        val inputMethodManager =
            context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        val shouldShow = !_uiState.value.isKeyboardShown
        if (shouldShow) {
            inputMethodManager?.showSoftInput(context.window.decorView, 0)
        } else {
            inputMethodManager?.hideSoftInputFromWindow(view.applicationWindowToken, 0)
        }
        _uiState.update { it.copy(isKeyboardShown = shouldShow) }
    }

    override fun onCleared() {
        super.onCleared()
        touchpadController.release()
    }
}