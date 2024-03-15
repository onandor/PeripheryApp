package com.onandor.peripheryapp.kbm.viewmodels

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Build
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.input.KeyMapping
import com.onandor.peripheryapp.kbm.input.KeyboardController
import com.onandor.peripheryapp.kbm.input.MouseButton
import com.onandor.peripheryapp.kbm.input.MultimediaController
import com.onandor.peripheryapp.kbm.input.TouchpadController
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class InputUiState(
    val hostName: String = "",
    val isKeyboardShown: Boolean = false,
    val keyboardInput: String = "",
    val keyboardLocale: Int = KeyMapping.Locales.EN_US,
    val deviceDisconnected: Boolean = false,
    val isExtendedKeyboardExpanded: Boolean = false,
    val toggledModifiers: Int = 0,
    val isExtendedKeyboardShown: Boolean = false,
    val isMultimediaControlShown: Boolean = false
)

@HiltViewModel
class InputViewModel @Inject constructor(
    private val touchpadController: TouchpadController,
    private val keyboardController: KeyboardController,
    private val multimediaController: MultimediaController,
    private val bluetoothController: BluetoothController,
    private val settings: Settings,
    private val navManager: INavigationManager
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

    private val hidProfileListener = object : BluetoothController.HidProfileListener {

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            if (state == BluetoothHidDevice.STATE_DISCONNECTED) {
                _uiState.update { it.copy(deviceDisconnected = true) }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            if (!registered) {
                _uiState.update { it.copy(deviceDisconnected = true) }
            }
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) { }
    }

    private val localeFlow = settings.observe(BtSettingKeys.KEYBOARD_LOCALE)
    private val extendedKeyboardFlow = settings.observe(BtSettingKeys.EXTENDED_KEYBOARD_SHOWN)

    private val _uiState = MutableStateFlow(InputUiState())
    val uiState = combine(
        _uiState, localeFlow, extendedKeyboardFlow
    ) { uiState, locale, extendedKeyboardShown ->
        uiState.copy(
            keyboardLocale = locale,
            isExtendedKeyboardShown = extendedKeyboardShown
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InputUiState()
        )

    init {
        bluetoothController.registerProfileListener(hidProfileListener)
        touchpadController.init()
        keyboardController.init()
        multimediaController.init()
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
        // Multimedia key codes
        if (event.keyCode in 0x55..0x5B || event.keyCode in 0x18..0x19) {
            multimediaController.sendMultimedia(event.keyCode, false)
            return
        }

        val imeModifier = if (event.isShiftPressed) {
            KeyMapping.Modifiers.L_SHIFT
        } else {
            KeyMapping.Modifiers.NONE
        }
        val modifier = _uiState.value.toggledModifiers or imeModifier

        val character = keyboardController.sendKey(modifier, event.keyCode)
        if (character == KeyMapping.BACKSPACE) {
            _uiState.update { it.copy(keyboardInput = it.keyboardInput.dropLast(1)) }
        } else {
            _uiState.update { it.copy(keyboardInput = it.keyboardInput + character) }
        }
        _uiState.update { it.copy(toggledModifiers = 0) }
        clearTextTimer.reset()
    }

    private fun specialCharacterPressed(event: KeyEvent) {
        if (event.characters == null) {
            return
        }
        val modifiers = _uiState.value.toggledModifiers
        val character = keyboardController.sendKey(modifiers, event.characters)
        if (character.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    keyboardInput = it.keyboardInput + character,
                    toggledModifiers = 0
                )
            }
            clearTextTimer.reset()
        }
    }

    fun onExtendedKeyPressed(scanCode: Int) {
        val modifiers = _uiState.value.toggledModifiers
        if (scanCode in KeyMapping.modifiers) {
            if (modifiers and scanCode != 0) {
                _uiState.update { it.copy(toggledModifiers = it.toggledModifiers xor scanCode) }
            } else {
                _uiState.update { it.copy(toggledModifiers = it.toggledModifiers or scanCode) }
            }
            return
        }
        // Lazy fix for the F7 == R_ALT modifier situation
        val _scanCode = if (scanCode == 0x40 + 0x9999) 0x40 else scanCode
        keyboardController.sendKeyWithScanCode(modifiers, _scanCode)
        _uiState.update { it.copy(toggledModifiers = 0) }
    }

    fun toggleKeyboard(context: Context) {
        val view = (context as Activity).findViewById<View>(android.R.id.content)
        val inputMethodManager =
            context.getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        val shouldShow = !_uiState.value.isKeyboardShown
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (shouldShow) {
                view.windowInsetsController?.show(WindowInsetsCompat.Type.ime())
            } else {
                view.windowInsetsController?.hide(WindowInsetsCompat.Type.ime())
            }
        } else {
            if (shouldShow) {
                context.window.decorView.requestFocus()
                inputMethodManager?.showSoftInput(context.window.decorView, 0)
            } else {
                inputMethodManager?.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            }
        }
        _uiState.update {
            it.copy(
                isKeyboardShown = shouldShow,
                isExtendedKeyboardExpanded = false
            )
        }
    }

    fun navigateToSettings() {
        navManager.navigateTo(NavActions.btSettings())
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    fun onToggleExtendedKeyboardExpanded() {
        _uiState.update { it.copy(isExtendedKeyboardExpanded = !it.isExtendedKeyboardExpanded) }
    }

    fun toggleMultimediaControl() {
        _uiState.update { it.copy(isMultimediaControlShown = !it.isMultimediaControlShown) }
    }

    fun onMultimediaKeyPressed(keyCode: Int) {
        multimediaController.sendMultimedia(keyCode, true)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.unregisterProfileListener(hidProfileListener)
        touchpadController.release()
        keyboardController.release()
        multimediaController.release()
    }
}