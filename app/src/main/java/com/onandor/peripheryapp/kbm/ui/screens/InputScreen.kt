package com.onandor.peripheryapp.kbm.ui.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.input.MouseButton
import com.onandor.peripheryapp.kbm.ui.components.ExtendedButtonGrid
import com.onandor.peripheryapp.kbm.ui.components.MultimediaControl
import com.onandor.peripheryapp.kbm.viewmodels.InputViewModel
import com.onandor.peripheryapp.ui.components.SwipeableSnackbarHost
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: InputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val deviceDisconnectedText = stringResource(R.string.input_snackbar_device_disconnected)
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            InputTopAppBar(
                hostName = uiState.hostName,
                onNavigateToSettings = {
                    if (uiState.isKeyboardShown) {
                        viewModel.toggleKeyboard(context)
                    }
                    viewModel.navigateToSettings()
                },
                onToggleKeyboard = { viewModel.toggleKeyboard(context) },
                onToggleMultimediaControl = viewModel::toggleMultimediaControl
            )
        },
        snackbarHost = {
            SwipeableSnackbarHost(hostState = snackbarHostState) {
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    ) { innerPadding ->
        val isKeyboardShown = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val density = LocalDensity.current

        LaunchedEffect(isKeyboardShown) {
            if (!isKeyboardShown && uiState.isKeyboardShown) {
                viewModel.keyboardDismissed()
            }
        }
        
        LaunchedEffect(uiState.deviceDisconnected) {
            if (uiState.deviceDisconnected) {
                coroutineScope.launch { snackbarHostState.showSnackbar(deviceDisconnectedText) }
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .imePadding()
        ) {
            InputReceiver(onKeyPressed = viewModel::onKeyPressed)
            TouchSurface(
                onButtonDown = viewModel::buttonDown,
                onButtonUp = viewModel::buttonUp,
                onMove = viewModel::move,
                onScroll = viewModel::scroll
            )
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomCenter),
                visible = uiState.isKeyboardShown && uiState.isExtendedKeyboardShown,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ExtendedButtonGrid(
                    expanded = uiState.isExtendedKeyboardExpanded,
                    toggledModifiers = uiState.toggledModifiers,
                    onToggleExpanded = viewModel::onToggleExtendedKeyboardExpanded,
                    onButtonClick = viewModel::onExtendedKeyPressed,
                    onButtonLongClick = viewModel::onExtendedKeyLongPressed
                )
            }
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.TopCenter),
                visible = uiState.isMultimediaControlShown,
                enter = slideInVertically { with(density) { -60.dp.roundToPx() } },
                exit = slideOutVertically { with(density) { -60.dp.roundToPx() } }
            ) {
                MultimediaControl(onButtonClick = { viewModel.onMultimediaKeyPressed(it) })
            }
            if (uiState.keyboardInput.isNotEmpty()) {
                KeyboardInputPreview(
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.BottomCenter),
                    input = uiState.keyboardInput
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
        color = MaterialTheme.colorScheme.surfaceVariant,
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
                            //Log.d("MotionEvent", "Left down")
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
                                    onScroll((focusY - prevFocusY) / 10f)
                                    //Log.d("MotionEvent", "Scroll")
                                }
                                inRightClick = false
                            } else {
                                onMove(focusX - prevFocusX, focusY - prevFocusY)
                                //Log.d("MotionEvent", "Move")
                            }
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (inRightClick) {
                            if (deltaTime <= tapTimeout) {
                                onButtonDown(MouseButton.RIGHT)
                                //Log.d("MotionEvent", "Right down")
                                onButtonUp(MouseButton.RIGHT)
                                //Log.d("MotionEvent", "Right up")
                                prevRightClick = true
                            }
                            inRightClick = false
                        } else {
                            if (inTap) {
                                if (!inDoubleTapHold) {
                                    // Only send down event if not holding left button after
                                    // double click
                                    onButtonDown(MouseButton.LEFT)
                                    //Log.d("MotionEvent", "Left down")
                                }
                                onButtonUp(MouseButton.LEFT)
                                //Log.d("MotionEvent", "Left up")
                            } else if (inDoubleTapHold) {
                                // If holding left button after double tap, send up event
                                onButtonUp(MouseButton.LEFT)
                                //Log.d("MotionEvent", "Left up")
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
    ) {
        Surface(
            modifier = Modifier
                .padding(10.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.primary
        ) {}
    }
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

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
private fun InputReceiver(
    onKeyPressed: (KeyEvent) -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra("event", KeyEvent::class.java)
                } else {
                    intent?.getParcelableExtra("event")
                }
                event?.let { onKeyPressed(it) }
            }
        }
        context.registerReceiver(receiver, IntentFilter("key_up"))
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputTopAppBar(
    hostName: String,
    onNavigateToSettings: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleMultimediaControl: () -> Unit
){
    TopAppBar(
        title = { Text(hostName) },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, "")
            }
            IconButton(onClick = onToggleMultimediaControl) {
                Icon(painterResource(id = R.drawable.ic_media_bluetooth_on), "")
            }
            IconButton(onClick = onToggleKeyboard) {
                Icon(painterResource(id = R.drawable.ic_keyboard_filled), "")
            }
        }
    )
}