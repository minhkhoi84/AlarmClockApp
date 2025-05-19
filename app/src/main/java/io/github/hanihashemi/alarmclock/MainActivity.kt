package io.github.hanihashemi.alarmclock

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.hanihashemi.alarmclock.R
import io.github.hanihashemi.alarmclock.ui.AnalogClockComponent
import io.github.hanihashemi.alarmclock.ui.shadow
import io.github.hanihashemi.alarmclock.ui.theme.AlarmClockTheme
import io.github.hanihashemi.alarmclock.ui.theme.NavigationBarColor
import io.github.hanihashemi.alarmclock.ui.theme.NavigationBarShadowColor
import kotlinx.coroutines.delay
import java.util.*
import io.github.hanihashemi.alarmclock.ui.TimerScreen
import io.github.hanihashemi.alarmclock.ui.StopwatchScreen
import io.github.hanihashemi.alarmclock.ui.AlarmScreen
import io.github.hanihashemi.alarmclock.ui.BedtimeScreen
import io.github.hanihashemi.alarmclock.ui.ClockScreen
import io.github.hanihashemi.alarmclock.ui.WorldClockScreen
import io.github.hanihashemi.alarmclock.ui.CountryClockScreen

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
            // You could show a message explaining why the app needs this permission
        }
    }

    private fun checkNotificationPermission() {
        // Check if we need to request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why the app needs this permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request the permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for notification permission
        checkNotificationPermission()
        
        // Handle app launch from alarm
        val isFromAlarm = intent.getBooleanExtra("ALARM_TRIGGERED", false)
        
        // Request to keep screen on if launched from alarm
        if (isFromAlarm) {
            Log.d("MainActivity", "App launched from alarm trigger")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }

        setContent {
            var selectedScreen by remember { mutableStateOf(0) }
            var currentScreen by remember { mutableStateOf<Screen>(Screen.MainClock) }
            
            AlarmClockTheme {
                when (val screen = currentScreen) {
                    is Screen.MainClock -> {
                        Scaffold(
                            bottomBar = {
                                NavigationBarComponent(
                                    selectedScreen = selectedScreen,
                                    onScreenSelected = { selectedScreen = it }
                                )
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                when (selectedScreen) {
                                    0 -> AlarmScreen()
                                    1 -> StopwatchScreen()
                                    2 -> ClockScreen(
                                        navigateToWorldClock = {
                                            currentScreen = Screen.WorldClock
                                        }
                                    )
                                    3 -> TimerScreen()
                                    4 -> BedtimeScreen()
                                    else -> AlarmScreen()
                                }
                            }
                        }
                    }
                    is Screen.WorldClock -> {
                        WorldClockScreen(
                            navigateToCountry = { countryName ->
                                currentScreen = Screen.CountryClock(countryName)
                            },
                            onBackPressed = {
                                currentScreen = Screen.MainClock
                            }
                        )
                    }
                    is Screen.CountryClock -> {
                        CountryClockScreen(
                            countryName = screen.countryName,
                            onBackPressed = {
                                currentScreen = Screen.WorldClock
                            }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle alarm trigger when app is already running
        if (intent.getBooleanExtra("ALARM_TRIGGERED", false)) {
            Log.d("MainActivity", "Received alarm trigger intent while running")
            // Keep screen on when alarm triggers
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }
    }
}

sealed class Screen {
    object MainClock : Screen()
    object WorldClock : Screen()
    data class CountryClock(val countryName: String) : Screen()
}

@Composable
fun HeaderComponent() {
    Box(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(text = "Clock", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun NavigationBarComponent(selectedScreen: Int, onScreenSelected: (Int) -> Unit) {
    NavigationBar(
        modifier = Modifier
            .shadow(
                color = NavigationBarShadowColor,
                offsetX = 0.dp,
                offsetY = (-5).dp,
                blurRadius = 50.dp
            )
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)),
        containerColor = NavigationBarColor
    ) {
        NavigationBarItem(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_alarm_24),
                contentDescription = null
            )
        }, selected = selectedScreen == 0, onClick = { onScreenSelected(0) })
        NavigationBarItem(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_hourglass_bottom_24),
                contentDescription = null
            )
        }, selected = selectedScreen == 1, onClick = { onScreenSelected(1) })
        NavigationBarItem(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_access_time_24),
                contentDescription = null
            )
        }, selected = selectedScreen == 2, onClick = { onScreenSelected(2) })
        NavigationBarItem(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_timer_24),
                contentDescription = null
            )
        }, selected = selectedScreen == 3, onClick = { onScreenSelected(3) })
        NavigationBarItem(icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_hotel_24),
                contentDescription = null
            )
        }, selected = selectedScreen == 4, onClick = { onScreenSelected(4) })
    }
}