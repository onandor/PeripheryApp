package com.onandor.peripheryapp.kbm.ui.screens

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.input.MouseButton
import com.onandor.peripheryapp.kbm.viewmodels.InputViewModel

@Composable
fun InputScreen(
    viewModel: InputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            InputTopAppBar(
                hostName = uiState.hostName,
                onToggleKeyboard = viewModel::toggleKeyboard
            )
        }
    ) { innerPadding ->
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val isKeyboardShown = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        LaunchedEffect(isKeyboardShown) {
            if (!isKeyboardShown && uiState.isKeyboardShown) {
                viewModel.keyboardDismissed()
            }
        }

        LaunchedEffect(uiState.isKeyboardShown) {
            if (uiState.isKeyboardShown) {
                focusRequester.requestFocus()
                keyboardController?.show()
            } else {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
        ) {
            BasicTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.key == Key.Backspace) {
                            viewModel.onBackspace()
                        }
                        false
                    },
                value = uiState.keyboardInputSink,
                onValueChange = viewModel::onKeyboardInputChanged,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                ),
                singleLine = false
            )
            TouchSurface(
                onButtonDown = viewModel::buttonDown,
                onButtonUp = viewModel::buttonUp,
                onMove = viewModel::move,
                onScroll = viewModel::scroll
            )
            if (uiState.keyboardInputDisplay.isNotEmpty()) {
                KeyboardInputPreview(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomCenter),
                    input = uiState.keyboardInputDisplay
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TouchSurface(
    modifier: Modifier = Modifier,
    onButtonDown: (MouseButton) -> Unit,
    onButtonUp: (MouseButton) -> Unit,
    onMove: (Float, Float) -> Unit,
    onScroll: (Float) -> Unit
) {
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
    var prevFocusX = 0f
    var prevFocusY = 0f

    Surface(
        color = Color.Blue,
        modifier = modifier
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
                            in doubleTapMinTime..doubleTapTimeout && !prevRightClick
                        ) {
                            // We are in a double tap and holding the button. The down event needs
                            // to be sent now to support dragging
                            inDoubleTapHold = true
                            onButtonDown(MouseButton.LEFT)
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
                            if (inRightClick && count > 1) {
                                if (!inDoubleTapHold) {
                                    onScroll((prevFocusY - focusY) / 10f)
                                    Log.d("MotionEvent", "Scroll")
                                }
                                inRightClick = false
                            } else {
                                onMove(focusX - prevFocusX, focusY - prevFocusY)
                                Log.d("MotionEvent", "Move")
                            }
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (inRightClick) {
                            if (deltaTime <= tapTimeout) {
                                onButtonDown(MouseButton.RIGHT)
                                Log.d("MotionEvent", "Right down")
                                onButtonUp(MouseButton.RIGHT)
                                Log.d("MotionEvent", "Right up")
                                prevRightClick = true
                            }
                            inRightClick = false
                        } else {
                            if (inTap) {
                                if (!inDoubleTapHold) {
                                    // Only send down event if not holding left button after
                                    // double click
                                    onButtonDown(MouseButton.LEFT)
                                    Log.d("MotionEvent", "Left down")
                                }
                                onButtonUp(MouseButton.LEFT)
                                Log.d("MotionEvent", "Left up")
                            } else if (inDoubleTapHold) {
                                // If holding left button after double tap, send up event
                                onButtonUp(MouseButton.LEFT)
                                Log.d("MotionEvent", "Left up")
                            }
                            prevRightClick = false
                        }
                        inTap = true
                        prevDownTime = event.downTime
                        inDoubleTapHold = false
                    }
                }
                prevFocusX = focusX
                prevFocusY = focusY
                true
            }
    ) { }
}

@Composable
private fun KeyboardInputPreview(
    modifier: Modifier,
    input: String
) {
    val lineHeight = 30
    val lineHeightDp = with(LocalDensity.current) { lineHeight.sp.toDp() }
    val padding = 10
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = padding.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ),
        color = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(
                min = 0.dp,
                max = 2 * lineHeightDp + 2 * padding.dp
            ),
            reverseLayout = true
        ) {
            item {
                Text(
                    modifier = Modifier.padding(padding.dp),
                    text = input,
                    lineHeight = lineHeight.sp,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputTopAppBar(
    hostName: String,
    onToggleKeyboard: () -> Unit
){
    TopAppBar(
        title = { Text(hostName) },
        actions = {
            IconButton(onClick = onToggleKeyboard) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_keyboard_filled),
                    contentDescription = stringResource(id = R.string.input_toggle_keyboard)
                )
            }
        }
    )
}