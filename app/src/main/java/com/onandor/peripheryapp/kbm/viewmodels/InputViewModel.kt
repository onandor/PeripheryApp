package com.onandor.peripheryapp.kbm.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.kbm.input.MouseButton
import com.onandor.peripheryapp.kbm.input.TouchpadController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class InputUiState(
    val asd: String = ""
)

@HiltViewModel
class InputViewModel @Inject constructor(
    private val touchpadController: TouchpadController
) : ViewModel() {

    init {
        touchpadController.init()
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

    override fun onCleared() {
        super.onCleared()
        touchpadController.release()
    }
}