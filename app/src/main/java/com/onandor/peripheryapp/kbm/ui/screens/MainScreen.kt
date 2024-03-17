package com.onandor.peripheryapp.kbm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.kbm.viewmodels.MainViewModel

@Composable
fun MainScreen(
    viewmodel: MainViewModel = hiltViewModel()
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            MenuButton(
                title = stringResource(id = R.string.main_bluetooth_kbm_title),
                description = stringResource(id = R.string.main_bluetooth_kbm_desc),
                icons = { KbmIcons() },
                onClick = viewmodel::navigateToBtKbmScreen
            )
            MenuButton(
                title = stringResource(id = R.string.main_wifi_webcam_title),
                description = stringResource(id = R.string.main_wifi_webcam_desc),
                icons = { WebcamIcons() },
                onClick = viewmodel::navigateToWifiWebcamScreen
            )
        }
    }
}

@Composable
private fun WebcamIcons() {
    Row {
        Icon(
            modifier = Modifier
                .size(35.dp)
                .padding(2.dp),
            painter = painterResource(id = R.drawable.ic_wifi),
            contentDescription = ""
        )
        Icon(
            modifier = Modifier
                .size(35.dp)
                .padding(2.dp),
            painter = painterResource(id = R.drawable.ic_video_camera_filled),
            contentDescription = ""
        )
    }
}

@Composable
private fun KbmIcons() {
    Row {
        Icon(
            modifier = Modifier
                .size(35.dp)
                .padding(2.dp),
            painter = painterResource(id = R.drawable.ic_bluetooth),
            contentDescription = ""
        )
        Icon(
            modifier = Modifier
                .size(35.dp)
                .padding(2.dp),
            painter = painterResource(id = R.drawable.ic_keyboard_filled),
            contentDescription = ""
        )
        Icon(
            modifier = Modifier
                .size(35.dp)
                .padding(2.dp),
            painter = painterResource(id = R.drawable.ic_mouse_filled),
            contentDescription = ""
        )
    }
}

@Composable
private fun MenuButton(
    title: String,
    description: String,
    icons: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(top = 20.dp, bottom = 50.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 25.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    icons()
                }
                Text(text = description)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp)
        ) {
            Surface(
                modifier = Modifier
                    .clickable { onClick() }
                    .clip(CircleShape)
                    .size(70.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    modifier = Modifier.padding(15.dp),
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = ""
                )
            }
        }
    }
}

@Composable
@Preview
fun PreviewMenuButton() {
    MenuButton(
        title = "WiFi webcam",
        description = "WiFi camera and microphone",
        icons = { },
        onClick = {}
    )
}