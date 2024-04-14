package com.onandor.peripheryapp.webcam.stream

enum class CameraSelection(val value: Int) {
    FRONT(0),
    BACK(1);

    companion object {

        fun fromInt(value: Int) = CameraSelection.entries.toList().first { it.value == value }
    }
}