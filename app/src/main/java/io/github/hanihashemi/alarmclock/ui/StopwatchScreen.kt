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
    var elapsedTime by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(0L) }
    var laps by remember { mutableStateOf(listOf<Long>()) }

    // Stopwatch logic
    LaunchedEffect(isRunning) {
        while (isRunning) {
            elapsedTime = System.currentTimeMillis() - startTime
            delay(10)
        }
    }

    val hours = (elapsedTime / 3600000).toInt()
    val minutes = ((elapsedTime / 60000) % 60).toInt()
    val seconds = ((elapsedTime / 1000) % 60).toInt()
    val millis = ((elapsedTime % 1000) / 10).toInt()

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Stopwatch", fontSize = 32.sp, modifier = Modifier.padding(bottom = 24.dp))
        Text("%02d:%02d:%02d.%02d".format(hours, minutes, seconds, millis), fontSize = 48.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Button(
                onClick = {
                    isRunning = true
                    startTime = System.currentTimeMillis()
                },
                enabled = !isRunning
            ) { Text("Start") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isRunning = false
                },
                enabled = isRunning
            ) { Text("Pause") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isRunning = false
                    elapsedTime = 0L
                },
                enabled = !isRunning && elapsedTime > 0L
            ) { Text("Reset") }
        }
    }
} 