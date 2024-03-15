package com.onandor.peripheryapp.kbm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.EmptyPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onandor.peripheryapp.kbm.input.ButtonData
import com.onandor.peripheryapp.kbm.input.ExtendedButtons
import com.onandor.peripheryapp.kbm.input.KeyMapping

private val EMPTY_LAMBDA: (Int) -> Unit = {}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Button(
    modifier: Modifier = Modifier,
    data: ButtonData,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit = EMPTY_LAMBDA,
    toggled: Boolean = false
) {
    val color = if (toggled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = {
                    onClick(data.scanCode)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onLongClick = {
                    if (onLongClick === EMPTY_LAMBDA) {
                        return@combinedClickable
                    }
                    onLongClick(data.scanCode)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .padding(2.dp),
        color = color
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = data.text)
        }
    }
}

@Composable
private fun ColumnScope.DefaultRows(
    toggledModifiers: Int,
    onButtonClick: (Int) -> Unit,
    onButtonLongClick: (Int) -> Unit
) {
    val modifier = Modifier
        .weight(1f)
        .aspectRatio(1f)

    Row {
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.ESCAPE, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.TAB, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.HOME, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.END, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.DELETE, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.Arrows.UP, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.BACKSPACE, onClick = onButtonClick)
    }
    Row {
        Button(
            modifier = modifier,
            data = ExtendedButtons.Modifiers.L_CTRL,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.L_CTRL != 0
        )
        Button(
            modifier = modifier,
            data = ExtendedButtons.Modifiers.SHIFT,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.L_SHIFT != 0
        )
        Button(
            modifier = modifier,
            data = ExtendedButtons.Modifiers.L_ALT,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.L_ALT != 0
        )
        Button(
            modifier = modifier,
            data = ExtendedButtons.Modifiers.R_ALT,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.R_ALT != 0
        )
        Button(modifier = modifier, data = ExtendedButtons.Arrows.LEFT, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.Arrows.DOWN, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.Arrows.RIGHT, onClick = onButtonClick)
    }
}

@Composable
private fun ColumnScope.ExpandedRows(
    toggledModifiers: Int,
    onButtonClick: (Int) -> Unit,
    onButtonLongClick: (Int) -> Unit
) {
    val modifier = Modifier
        .weight(1f)
        .aspectRatio(1f)

    Row {
        Button(modifier = modifier, data = ExtendedButtons.FRow.F1, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F2, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F3, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F4, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F5, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F6, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F7, onClick = onButtonClick)
    }
    Row {
        Button(modifier = modifier, data = ExtendedButtons.FRow.F8, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F9, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F10, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F11, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.FRow.F12, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.INSERT, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.PAGE_UP, onClick = onButtonClick)
    }
    Row {
        Button(
            modifier = modifier,
            data = ExtendedButtons.Modifiers.L_META,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.L_META != 0
        )
        Button(modifier = modifier,
            data = ExtendedButtons.Modifiers.R_META,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.R_META != 0
        )
        Button(modifier = modifier,
            data = ExtendedButtons.Modifiers.R_CTRL,
            onClick = onButtonLongClick,
            onLongClick = onButtonClick,
            toggled = toggledModifiers and KeyMapping.Modifiers.R_CTRL != 0
        )
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.PRINT_SCR, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.SCRLK, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.PAUSE, onClick = onButtonClick)
        Button(modifier = modifier, data = ExtendedButtons.SpecialKeys.PAGE_DOWN, onClick = onButtonClick)
    }
}

@Composable
fun ExtendedButtonGrid(
    expanded: Boolean,
    toggledModifiers: Int,
    onToggleExpanded: () -> Unit,
    onButtonClick: (Int) -> Unit,
    onButtonLongClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            modifier = Modifier
                .padding(10.dp)
                .height(40.dp)
                .aspectRatio(1f)
                .clickable { onToggleExpanded() }
                .clip(CircleShape),
        ) {
            if (expanded) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, "")
            } else {
                Icon(imageVector = Icons.Default.KeyboardArrowUp, "")
            }
        }

        Surface(
            modifier = Modifier.clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
        ) {
            Column(
                modifier = Modifier.padding(2.dp)
            ) {
                AnimatedVisibility(visible = expanded) {
                    Column {
                        ExpandedRows(
                            toggledModifiers = toggledModifiers,
                            onButtonClick = onButtonClick,
                            onButtonLongClick = onButtonLongClick
                        )
                    }
                }
                DefaultRows(
                    toggledModifiers = toggledModifiers,
                    onButtonClick = onButtonClick,
                    onButtonLongClick = onButtonLongClick
                )
            }
        }
    }
}

@Composable
@Preview
private fun PreviewButton() {
    val data = ButtonData("L Ctrl", 0)
    Button(data = data, onClick = {})
}

@Preview
@Composable
private fun PreviewButtonGrid() {
    ExtendedButtonGrid(
        expanded = true,
        toggledModifiers = 0,
        onToggleExpanded = {},
        onButtonClick = {},
        onButtonLongClick = {}
    )
}