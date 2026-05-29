package io.github.jqssun.airplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvIdleScreen(
    deviceName: String,
    pin: String?,
    isRunning: Boolean,
    connectionCount: Int,
    onSettings: () -> Unit,
) {
    val settingsFocusRequester = remember { FocusRequester() }
    val connected = connectionCount > 0

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (connected) Color(0xFF001A0A) else Color(0xFF0A0A1E)),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TvText(
                    text = when {
                        connected -> "Mirroring Active"
                        isRunning -> "Ready to Mirror"
                        else      -> "Starting…"
                    },
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connected) Color(0xFF44FF88) else Color.White,
                )
                Spacer(Modifier.height(8.dp))
                TvText(
                    text = "Device name: $deviceName",
                    fontSize = 24.sp,
                    color = Color(0xFFAAAAAA),
                )
                if (!pin.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.width(200.dp),
                        color = Color(0xFF444466),
                    )
                    Spacer(Modifier.height(8.dp))
                    TvText(
                        text = "PIN: $pin",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF88AAFF),
                    )
                    TvText(
                        text = "Enter this PIN on your iPad when prompted",
                        fontSize = 18.sp,
                        color = Color(0xFF777777),
                    )
                }
                Spacer(Modifier.height(8.dp))
                TvText(
                    text = "On your iPad: Control Center → Screen Mirroring → $deviceName",
                    fontSize = 18.sp,
                    color = Color(0xFF666666),
                )
            }

            Button(
                onClick = onSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
                    .focusRequester(settingsFocusRequester),
            ) {
                TvText("⚙  Settings")
            }
        }
    }

    LaunchedEffect(Unit) {
        settingsFocusRequester.requestFocus()
    }
}
