package io.github.hanihashemi.alarmclock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen() {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableStateOf(0L) }
    var lastTimestamp by remember { mutableStateOf(0L) }

    // Stopwatch logic
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10)
            elapsedMillis += 10
        }
    }

    val hours = (elapsedMillis / 3600000).toInt()
    val minutes = ((elapsedMillis / 60000) % 60).toInt()
    val seconds = ((elapsedMillis / 1000) % 60).toInt()
    val millis = ((elapsedMillis % 1000) / 10).toInt()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bấm giờ", fontSize = 32.sp, modifier = Modifier.padding(bottom = 24.dp))
        Text("%02d:%02d:%02d.%02d".format(hours, minutes, seconds, millis), fontSize = 48.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Button(
                onClick = {
                    isRunning = true
                },
                enabled = !isRunning
            ) { Text("Bắt đầu") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isRunning = false
                },
                enabled = isRunning
            ) { Text("Tạm dừng") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isRunning = false
                    elapsedMillis = 0L
                },
                enabled = !isRunning && elapsedMillis > 0L
            ) { Text("Đặt lại") }
        }
    }
} 