package com.onandor.peripheryapp.kbm.ui.screens

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.kbm.viewmodels.InputViewModel


@Composable
fun InputScreen(
    viewModel: InputViewModel = hiltViewModel()
) {

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TouchSurface()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TouchSurface() {
    var inTap = true
    var inRightClick = false
    val viewConfiguration = LocalViewConfiguration.current
    val slopLimit = viewConfiguration.touchSlop * viewConfiguration.touchSlop
    val tapTimeout = 100
    val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
    val doubleTapMinTime = viewConfiguration.doubleTapMinTimeMillis
    var downFocusX = 0f
    var downFocusY = 0f
    var prevDownTime = 0L
    var inDoubleTapHold = false
    var prevRightClick = false

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInteropFilter { event ->
                val action = event.actionMasked
                val pointerUp = action == MotionEvent.ACTION_POINTER_UP
                val skipIndex = if (pointerUp) event.actionIndex else -1
                var sumX = 0f
                var sumY = 0f
                val count = event.pointerCount
                val deltaTime = event.eventTime - event.downTime

                for (i in 0.rangeUntil(count)) {
                    if (skipIndex == i) continue
                    sumX += event.getX(i)
                    sumY += event.getY(i)
                }
                val div = if (pointerUp) count - 1 else count
                val focusX = sumX / div
                val focusY = sumY / div

                if (count > 1 && !pointerUp) {
                    inRightClick = true
                }

                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        downFocusX = focusX
                        downFocusY = focusY
                        if (event.downTime - prevDownTime
                            in doubleTapMinTime..doubleTapTimeout && !prevRightClick) {
                            // We are in a double tap and holding the button. The down event needs
                            // to be sent now to support dragging
                            inDoubleTapHold = true
                            Log.d("MotionEvent", "Left down")
                        }
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        downFocusX = focusX
                        downFocusY = focusY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (inTap) {
                            val deltaX = (focusX - downFocusX).toInt()
                            val deltaY = (focusY - downFocusY).toInt()
                            val distance = (deltaX * deltaX) + (deltaY * deltaY)
                            if (distance > slopLimit) {
                                inTap = false
                            }
                        }

                        if (!inTap) {
                            if (inRightClick  && count > 1) {
                                if (!inDoubleTapHold) {
                                    Log.d("MotionEvent", "Scroll")
                                }
                                inRightClick = false
                            } else {
                                Log.d("MotionEvent", "Move")
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (inRightClick) {
                            if (deltaTime <= tapTimeout) {
                                Log.d("MotionEvent", "Right down")
                                Log.d("MotionEvent", "Right up")
                                prevRightClick = true
                            }
                            inRightClick = false
                        } else {
                            if (inTap) {
                                if (!inDoubleTapHold) {
                                    // Only send down event if not holding left button after
                                    // double click
                                    Log.d("MotionEvent", "Left down")
                                }
                                Log.d("MotionEvent", "Left up")
                            } else if (inDoubleTapHold) {
                                // If holding left button after double tap, send up event
                                Log.d("MotionEvent", "Left up")
                            }
                            prevRightClick = false
                        }
                        inTap = true
                        prevDownTime = event.downTime
                        inDoubleTapHold = false
                    }
                }
                true
            }
    ) {

    }
}