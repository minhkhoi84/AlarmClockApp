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
import io.github.hanihashemi.alarmclock.ui.NoteScreen

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
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            var hour by remember { mutableStateOf("0") }
            var minute by remember { mutableStateOf("0") }
            var second by remember { mutableStateOf("0") }
            var amOrPm by remember { mutableStateOf("0") }

            LaunchedEffect(Unit) {
                while (true) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
                    hour = cal.get(Calendar.HOUR).run {
                        if (this.toString().length == 1) "0$this" else "$this"
                    }
                    minute = cal.get(Calendar.MINUTE).run {
                        if (this.toString().length == 1) "0$this" else "$this"
                    }
                    second = cal.get(Calendar.SECOND).run {
                        if (this.toString().length == 1) "0$this" else "$this"
                    }
                    amOrPm = cal.get(Calendar.AM_PM).run {
                        if (this == Calendar.AM) "AM" else "PM"
                    }

                    delay(1000)
                }
            }

            AlarmClockTheme {
                // Set initial screen to Alarm if launched from alarm notification
                var selectedScreen by remember { mutableStateOf(if (isFromAlarm) 0 else 2) }
                
                // Check if alarm is ringing
                var isAlarmRinging by remember { mutableStateOf(false) }
                
                // Periodically check if alarm is ringing
                LaunchedEffect(Unit) {
                    while(true) {
                        isAlarmRinging = (applicationContext as AlarmClockApplication).mediaPlayer != null
                        delay(1000)
                    }
                }
                
                // If alarm is ringing and app was launched from alarm notification,
                // show a fullscreen alarm stop UI
                if (isFromAlarm && isAlarmRinging) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF3B30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Current time
                            Text(
                                text = "$hour:$minute",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = amOrPm,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(64.dp))
                            
                            Text(
                                text = "BÁO THỨC",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(64.dp))
                            
                            Button(
                                onClick = {
                                    try {
                                        // Stop alarm through application class
                                        (applicationContext as AlarmClockApplication).stopAlarm()
                                        
                                        // Also stop service
                                        val intent = Intent(applicationContext, AlarmService::class.java)
                                        stopService(intent)
                                        
                                        // Send stop action broadcast
                                        val stopIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
                                            action = AlarmReceiver.ACTION_STOP_ALARM
                                        }
                                        sendBroadcast(stopIntent)
                                        
                                        Toast.makeText(applicationContext, "Đã tắt báo thức", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error stopping alarm: ${e.message}", e)
                                        Toast.makeText(applicationContext, "Lỗi khi tắt báo thức: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(40.dp)
                            ) {
                                Text(
                                    "TẮT BÁO THỨC",
                                    color = Color(0xFFFF3B30),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                } else {
                    Scaffold(bottomBar = {
                        NavigationBarComponent(selectedScreen = selectedScreen, onScreenSelected = { selectedScreen = it })
                    }) {
                        Box(
                            modifier = Modifier
                                .padding(it)
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (selectedScreen) {
                                0 -> AlarmScreen()
                                1 -> TimerScreen()
                                2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    HeaderComponent()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .fillMaxHeight(fraction = 0.8f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AnalogClockComponent(
                                                hour = hour.toInt(),
                                                minute = minute.toInt(),
                                                second = second.toInt()
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            DigitalClockComponent(
                                                hour = hour,
                                                minute = minute,
                                                amOrPm = amOrPm,
                                            )
                                        }
                                    }
                                }
                                3 -> StopwatchScreen()
                                4 -> NoteScreen()
                                // Thêm các màn hình khác nếu muốn
                                else -> HeaderComponent()
                            }
                        }
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
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
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

@Composable
fun DigitalClockComponent(
    hour: String,
    minute: String,
    amOrPm: String,
) {
    Text(
        text = "$hour:$minute $amOrPm", style = MaterialTheme.typography.titleLarge
    )
    Text(
        text = "Hà Nội, Việt Nam", style = MaterialTheme.typography.bodyMedium.merge(
            TextStyle(
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.6f
                )
            )
        )
    )
}