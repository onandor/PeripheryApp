package com.onandor.peripheryapp.kbm.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.onandor.peripheryapp.R

@Composable
private fun MultimediaButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .clip(CircleShape),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
        }
    }
}
@Composable
fun MultimediaControl(onButtonClick: (Int) -> Unit) {
    Surface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_MEDIA_PREVIOUS) }) {
                Icon(painterResource(id = R.drawable.ic_previous_filled), "")
            }
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }) {
                Row {
                    Icon(Icons.Default.PlayArrow, "")
                    Icon(painterResource(id = R.drawable.ic_pause), "")
                }
            }
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_MEDIA_STOP) }) {
                Icon(painterResource(id = R.drawable.ic_stop_filled), "")
            }
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_MEDIA_NEXT) }) {
                Icon(painterResource(id = R.drawable.ic_next_filled), "")
            }
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_VOLUME_MUTE) }) {
                Icon(painterResource(id = R.drawable.ic_mute_filled), "")
            }
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_VOLUME_DOWN) }) {
                Icon(painterResource(id = R.drawable.ic_volume_down_filled), "")
            }
            MultimediaButton(onClick = { onButtonClick(KeyEvent.KEYCODE_VOLUME_DOWN) }) {
                Icon(painterResource(id = R.drawable.ic_volume_up_filled), "")
            }
        }
    }
}