package io.github.hanihashemi.alarmclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(
    navigateToWorldClock: () -> Unit
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    val timeZoneId = "Asia/Ho_Chi_Minh" // Default to Vietnam
    var showCountryClock by remember { mutableStateOf<String?>(null) }
    
    // Update time every second
    LaunchedEffect(key1 = Unit) {
        while (true) {
            currentTime = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
            kotlinx.coroutines.delay(1000)
        }
    }

    // Display either main clock or country-specific clock
    if (showCountryClock != null) {
        CountryClockScreen(
            countryName = showCountryClock!!,
            onBackPressed = { showCountryClock = null }
        )
    } else {
        MainClockScreen(
            currentTime = currentTime, 
            timeZoneId = timeZoneId,
            navigateToWorldClock = navigateToWorldClock
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainClockScreen(
    currentTime: Calendar,
    timeZoneId: String,
    navigateToWorldClock: () -> Unit
) {
    val hour = currentTime.get(Calendar.HOUR)
    val minute = currentTime.get(Calendar.MINUTE)
    val second = currentTime.get(Calendar.SECOND)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    timeFormat.timeZone = TimeZone.getTimeZone(timeZoneId)
    val formattedTime = timeFormat.format(currentTime.time)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clock") },
                actions = {
                    // Text button instead of icon button
                    TextButton(onClick = navigateToWorldClock) {
                        Text("World", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Analog Clock
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnalogClockComponent(
                    hour = hour,
                    minute = minute,
                    second = second
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Digital time
            Text(
                text = formattedTime,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Hà Nội, Việt Nam",
                fontSize = 18.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
} 