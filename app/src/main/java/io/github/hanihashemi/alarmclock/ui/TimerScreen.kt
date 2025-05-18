package io.github.hanihashemi.alarmclock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen() {
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var inputHours by remember { mutableStateOf("") }
    var inputMinutes by remember { mutableStateOf("") }
    var inputSeconds by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var totalSeconds by remember { mutableStateOf(0) }
    var initialTotalSeconds by remember { mutableStateOf(0) }

    // Timer logic
    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused && totalSeconds > 0) {
            delay(1000)
            totalSeconds--
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            hours = h
            minutes = m
            seconds = s
            if (totalSeconds == 0) {
                isRunning = false
                isPaused = false
                // Simple alert, you can replace with sound or notification
                // (In real app, use proper notification)
                // For Compose Desktop: JOptionPane.showMessageDialog(null, "Time's up!")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Timer", fontSize = 32.sp, modifier = Modifier.padding(bottom = 24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputHours,
                onValueChange = { if (it.all { c -> c.isDigit() }) inputHours = it },
                label = { Text("Giờ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = inputMinutes,
                onValueChange = { if (it.all { c -> c.isDigit() }) inputMinutes = it },
                label = { Text("Phút") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = inputSeconds,
                onValueChange = { if (it.all { c -> c.isDigit() }) inputSeconds = it },
                label = { Text("Giây") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("%02d:%02d:%02d".format(hours, minutes, seconds), fontSize = 48.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Row {
            Button(
                onClick = {
                    val h = inputHours.toIntOrNull() ?: 0
                    val m = inputMinutes.toIntOrNull() ?: 0
                    val s = inputSeconds.toIntOrNull() ?: 0
                    val total = h * 3600 + m * 60 + s
                    if (total > 0) {
                        hours = h
                        minutes = m
                        seconds = s
                        totalSeconds = total
                        initialTotalSeconds = total
                        isRunning = true
                        isPaused = false
                    }
                },
                enabled = !isRunning || totalSeconds == 0
            ) { Text("Bắt đầu") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { isPaused = true },
                enabled = isRunning && !isPaused
            ) { Text("Tạm dừng") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { isPaused = false },
                enabled = isRunning && isPaused
            ) { Text("Tiếp tục") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    isRunning = false
                    isPaused = false
                    totalSeconds = initialTotalSeconds
                    val h = totalSeconds / 3600
                    val m = (totalSeconds % 3600) / 60
                    val s = totalSeconds % 60
                    hours = h
                    minutes = m
                    seconds = s
                },
                enabled = isRunning || isPaused
            ) { Text("Đặt lại") }
        }
        if (totalSeconds == 0 && initialTotalSeconds > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Hết giờ!", color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
        }
    }
} 