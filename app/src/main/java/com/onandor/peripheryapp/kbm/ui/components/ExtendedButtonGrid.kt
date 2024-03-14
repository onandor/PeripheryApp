package com.onandor.peripheryapp.kbm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onandor.peripheryapp.kbm.input.ButtonData
import com.onandor.peripheryapp.kbm.input.ExtendedButtons
import com.onandor.peripheryapp.kbm.input.KeyMapping
import kotlin.math.exp

@Composable
fun Button(
    modifier: Modifier = Modifier,
    data: ButtonData,
    onClick: (Int) -> Unit,
    toggled: Boolean = false
) {
    val color = if (toggled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = modifier
            .clickable { onClick(data.scanCode) }
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
private fun DefaultRow(
    buttonModifier: Modifier,
    toggledModifiers: Int,
    onButtonClick: (Int) -> Unit
) {
    Surface {
        Column {
            Row {
                Button(modifier = buttonModifier, data = ExtendedButtons.SpecialKeys.ESCAPE, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.SpecialKeys.TAB, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.SpecialKeys.HOME, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.SpecialKeys.END, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.SpecialKeys.DELETE, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.Arrows.UP, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.SpecialKeys.BACKSPACE, onClick = onButtonClick)
            }
            Row {
                Button(
                    modifier = buttonModifier,
                    data = ExtendedButtons.Modifiers.L_CTRL,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.L_CTRL != 0
                )
                Button(
                    modifier = buttonModifier,
                    data = ExtendedButtons.Modifiers.SHIFT,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.L_SHIFT != 0
                )
                Button(
                    modifier = buttonModifier,
                    data = ExtendedButtons.Modifiers.L_ALT,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.L_ALT != 0
                )
                Button(
                    modifier = buttonModifier,
                    data = ExtendedButtons.Modifiers.R_ALT,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.R_ALT != 0
                )
                Button(modifier = buttonModifier, data = ExtendedButtons.Arrows.LEFT, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.Arrows.DOWN, onClick = onButtonClick)
                Button(modifier = buttonModifier, data = ExtendedButtons.Arrows.RIGHT, onClick = onButtonClick)
            }
        }
    }
}

@Composable
private fun ExpandedRow(
    btnModifier: Modifier,
    toggledModifiers: Int,
    onButtonClick: (Int) -> Unit
) {
    Surface {
        Column {
            Row {
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F1, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F2, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F3, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F4, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F5, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F6, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F7, onClick = onButtonClick)
            }
            Row {
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F8, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F9, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F10, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F11, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.FRow.F12, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.SpecialKeys.INSERT, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.SpecialKeys.PAGE_UP, onClick = onButtonClick)
            }
            Row {
                Button(
                    modifier = btnModifier,
                    data = ExtendedButtons.Modifiers.L_META,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.L_META != 0
                )
                Button(modifier = btnModifier,
                    data = ExtendedButtons.Modifiers.R_META,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.R_META != 0
                )
                Button(modifier = btnModifier,
                    data = ExtendedButtons.Modifiers.R_CTRL,
                    onClick = onButtonClick,
                    toggled = toggledModifiers and KeyMapping.Modifiers.R_CTRL != 0
                )
                Button(modifier = btnModifier, data = ExtendedButtons.SpecialKeys.PRINT_SCR, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.SpecialKeys.SCRLK, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.SpecialKeys.PAUSE, onClick = onButtonClick)
                Button(modifier = btnModifier, data = ExtendedButtons.SpecialKeys.PAGE_DOWN, onClick = onButtonClick)
            }
        }
    }
}

@Composable
fun ExtendedButtonGrid(
    expanded: Boolean,
    toggledModifiers: Int,
    onToggleExpanded: () -> Unit,
    onButtonClick: (Int) -> Unit
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
        val buttonModifier = Modifier
            .weight(1f)
            .aspectRatio(1f)

        AnimatedVisibility(visible = expanded) {
            ExpandedRow(
                btnModifier = buttonModifier,
                toggledModifiers = toggledModifiers,
                onButtonClick = onButtonClick
            )
        }
        DefaultRow(
            buttonModifier = buttonModifier,
            toggledModifiers = toggledModifiers,
            onButtonClick = onButtonClick
        )
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
        onButtonClick = {}
    )
}