package io.github.hanihashemi.alarmclock.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.hanihashemi.alarmclock.AlarmReceiver
import java.util.*
import android.app.TimePickerDialog
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import io.github.hanihashemi.alarmclock.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import io.github.hanihashemi.alarmclock.AlarmClockApplication
import io.github.hanihashemi.alarmclock.AlarmService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(listOf<AlarmItem>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempHour by remember { mutableStateOf(6) }
    var tempMinute by remember { mutableStateOf(0) }
    var is24HourFormat by remember { mutableStateOf(false) }
    var isPm by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    
    // State to track if alarm is currently ringing
    var isAlarmRinging by remember { mutableStateOf(false) }
    
    // MediaPlayer for testing sound
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Check if an alarm is currently ringing by querying application
    LaunchedEffect(Unit) {
        isAlarmRinging = (context.applicationContext as? AlarmClockApplication)?.mediaPlayer != null
        
        // Check status periodically
        while(true) {
            delay(1000)
            isAlarmRinging = (context.applicationContext as? AlarmClockApplication)?.mediaPlayer != null
        }
    }

    if (showTimePicker) {
        ModernTimePickerDialog(
            onDismiss = { 
                showTimePicker = false
                editingAlarm = null
            },
            onConfirm = { hour, minute ->
                try {
                    Toast.makeText(context, "Starting to set alarm", Toast.LENGTH_SHORT).show()
                    if (editingAlarm != null) {
                        alarms = alarms.map {
                            if (it.id == editingAlarm!!.id) it.copy(hour = hour, minute = minute) else it
                        }
                        setAlarm(context, hour, minute, editingAlarm!!.id)
                        editingAlarm = null
                    } else {
                        val newId = (alarms.maxOfOrNull { it.id } ?: 0) + 1
                        alarms = alarms + AlarmItem(newId, hour, minute, true)
                        setAlarm(context, hour, minute, newId)
                    }
                    showTimePicker = false
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            initialHour = tempHour,
            initialMinute = tempMinute,
            is24HourFormat = is24HourFormat
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Alarm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(alarms) { alarm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp
                            )
                            Text(
                                text = "Alarm",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = alarm.enabled,
                            onCheckedChange = { checked ->
                                alarms = alarms.map {
                                    if (it.id == alarm.id) it.copy(enabled = checked) else it
                                }
                                if (checked) {
                                    setAlarm(context, alarm.hour, alarm.minute, alarm.id)
                                } else {
                                    cancelAlarm(context, alarm.id)
                                }
                            }
                        )
                    }
                    Divider()
                }
            }
            Spacer(modifier = Modifier.height(60.dp)) // To avoid being covered by FAB
        }
        // Add alarm button (FloatingActionButton)
        FloatingActionButton(
            onClick = {
                editingAlarm = null
                tempHour = 6
                tempMinute = 0
                showTimePicker = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add alarm", tint = Color.White)
        }
    }
    
    // Clean up MediaPlayer when screen is destroyed
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}

@Composable
fun ModernTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 0,
    initialMinute: Int = 0,
    is24HourFormat: Boolean = false
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(initialHour >= 12) }
    var isSelectingHour by remember { mutableStateOf(true) }
    
    val displayHour = when {
        is24HourFormat -> hour
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF121212)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color.White
                )
                
                // Digital time display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelectingHour) Color(0xFF1F3B5B) else Color(0xFF2A2A2A))
                            .clickable { isSelectingHour = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", displayHour),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Text(
                        text = ":",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        color = Color.White
                    )
                    
                    // Minute selector
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isSelectingHour) Color(0xFF1F3B5B) else Color(0xFF2A2A2A))
                            .clickable { isSelectingHour = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", minute),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    // AM/PM selector (only for 12-hour format)
                    if (!is24HourFormat) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier
                                .width(60.dp) // Increase size for easier clicking
                                .height(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF3F2E5C), RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(if (!isPm) Color(0xFF3F2E5C) else Color(0xFF1A1A1A))
                                    .clickable { 
                                        isPm = false
                                        // If previously PM, switch to AM
                                        if (hour >= 12) {
                                            hour -= 12
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AM",
                                    fontSize = 16.sp,
                                    fontWeight = if (!isPm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!isPm) Color.White else Color.Gray
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(if (isPm) Color(0xFF3F2E5C) else Color(0xFF1A1A1A))
                                    .clickable { 
                                        isPm = true
                                        // If previously AM, switch to PM
                                        if (hour < 12) {
                                            hour += 12
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PM",
                                    fontSize = 16.sp,
                                    fontWeight = if (isPm) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPm) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Analog clock
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Clock face
                    AnalogClockFace(
                        hour = hour % 12,
                        minute = minute,
                        isSelectingHour = isSelectingHour,
                        onTimeSelected = { h, m ->
                            if (isSelectingHour) {
                                hour = if (isPm && !is24HourFormat) {
                                    if (h == 12) 12 else h + 12
                                } else if (!isPm && !is24HourFormat) {
                                    if (h == 12) 0 else h
                                } else {
                                    h
                                }
                                // Automatically switch to minute selection after choosing hour
                                isSelectingHour = false
                            } else {
                                minute = m
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF90CAF9))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            // Process AM/PM time correctly
                            val finalHour = if (!is24HourFormat) {
                                if (isPm && displayHour < 12) {
                                    displayHour + 12
                                } else if (!isPm && displayHour == 12) {
                                    0
                                } else if (isPm && displayHour == 12) {
                                    12
                                } else {
                                    displayHour
                                }
                            } else {
                                hour
                            }
                            onConfirm(finalHour, minute)
                        }
                    ) {
                        Text("OK", color = Color(0xFF90CAF9))
                    }
                }
            }
        }
    }
}

@Composable
fun AnalogClockFace(
    hour: Int,
    minute: Int,
    isSelectingHour: Boolean,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val selectedHour = remember { mutableStateOf(hour) }
    val selectedMinute = remember { mutableStateOf(minute) }
    
    // Update state when props change
    LaunchedEffect(hour, minute) {
        selectedHour.value = hour
        selectedMinute.value = minute
    }
    
    Box(
        modifier = Modifier
            .size(240.dp)
            .clickable { /* This makes the entire area clickable */ }
    ) {
        // Clock background circle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2A2A2A), CircleShape)
        )
        
        if (isSelectingHour) {
            // Draw hour indicators when selecting hour
            for (i in 1..12) {
                val angle = (i * 30 - 90) * (PI / 180f)
                val radius = 100.dp // slightly smaller than the clock face
                val x = cos(angle).toFloat()
                val y = sin(angle).toFloat()
                
                // Calculate position
                val xPos = 120.dp + radius * x
                val yPos = 120.dp + radius * y
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .absoluteOffset(
                            x = xPos - 16.dp, // Center the box
                            y = yPos - 16.dp
                        )
                        .clickable {
                            selectedHour.value = if (i == 12) 0 else i
                            onTimeSelected(if (i == 12) 0 else i, selectedMinute.value)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Draw highlighted hour (current position)
                    if (i == selectedHour.value % 12 || (i == 12 && selectedHour.value % 12 == 0)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF90CAF9), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (i == 12) "12" else i.toString(),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Draw regular hour
                        Text(
                            text = if (i == 12) "12" else i.toString(),
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Draw minute indicators when selecting minute
            // Minute markers in steps of 5
            for (i in 0..11) {
                val minute = i * 5
                val angle = (i * 30 - 90) * (PI / 180f)
                val radius = 100.dp 
                val x = cos(angle).toFloat()
                val y = sin(angle).toFloat()
                
                val xPos = 120.dp + radius * x
                val yPos = 120.dp + radius * y
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .absoluteOffset(
                            x = xPos - 16.dp,
                            y = yPos - 16.dp
                        )
                        .clickable {
                            selectedMinute.value = minute
                            onTimeSelected(selectedHour.value, minute)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Draw highlighted minute marker
                    if (minute == selectedMinute.value) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF90CAF9), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d", minute),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Draw regular minute marker
                        Text(
                            text = String.format("%02d", minute),
                            color = Color.White
                        )
                    }
                }
            }
            
            // Additional minute markers for more precise selection
            for (i in 0..59) {
                if (i % 5 != 0) { // Skip the main markers we've already drawn
                    val angle = (i * 6 - 90) * (PI / 180f)
                    val radius = 100.dp
                    val x = cos(angle).toFloat()
                    val y = sin(angle).toFloat()
                    
                    val xPos = 120.dp + radius * x
                    val yPos = 120.dp + radius * y
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .absoluteOffset(
                                x = xPos - 6.dp,
                                y = yPos - 6.dp
                            )
                            .clickable {
                                selectedMinute.value = i
                                onTimeSelected(selectedHour.value, i)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Small dots for minutes
                        if (i == selectedMinute.value) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF90CAF9), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color.Gray, CircleShape)
                            )
                        }
                    }
                }
            }
        }
        
        // Draw clock hands with Canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 16.dp.toPx()
            
            // Draw different hands based on what we're selecting
            if (isSelectingHour) {
                // Hour hand
                val hourAngle = ((selectedHour.value % 12) * 30 - 90) * (PI / 180f)
                val hourHandLength = radius * 0.5f
                drawLine(
                    color = Color(0xFF90CAF9),
                    start = center,
                    end = Offset(
                        center.x + hourHandLength * cos(hourAngle).toFloat(),
                        center.y + hourHandLength * sin(hourAngle).toFloat()
                    ),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            } else {
                // Minute hand
                val minuteAngle = (selectedMinute.value * 6 - 90) * (PI / 180f)
                val minuteHandLength = radius * 0.7f
                drawLine(
                    color = Color(0xFF90CAF9),
                    start = center,
                    end = Offset(
                        center.x + minuteHandLength * cos(minuteAngle).toFloat(),
                        center.y + minuteHandLength * sin(minuteAngle).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // Center dot
            drawCircle(
                color = Color(0xFF90CAF9),
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun CustomNumberSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    suffix: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { 
                if (value < range.last) {
                    onValueChange(value + 1)
                }
            }
        ) {
            Text("▲", style = MaterialTheme.typography.titleMedium)
        }
        
        Text(
            text = "$value $suffix",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        IconButton(
            onClick = { 
                if (value > range.first) {
                    onValueChange(value - 1)
                }
            }
        ) {
            Text("▼", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Immutable
data class AlarmItem(val id: Int, val hour: Int, val minute: Int, val enabled: Boolean)

fun setAlarm(context: Context, hour: Int, minute: Int, id: Int) {
    try {
        Log.d("AlarmScreen", "Setting alarm for $hour:$minute, id=$id")
        Toast.makeText(context, "Starting to set alarm", Toast.LENGTH_SHORT).show()
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e("AlarmScreen", "AlarmManager null")
            Toast.makeText(context, "AlarmManager null", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create explicit intent for AlarmReceiver
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", id)
            // Add flags to ensure delivery even if app is stopped
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        
        // Create pending intent with proper flags
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            id, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        Log.d("AlarmScreen", "PendingIntent created")
        Toast.makeText(context, "PendingIntent OK", Toast.LENGTH_SHORT).show()
        
        // Setup calendar for alarm time
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (before(now)) {
                add(Calendar.DATE, 1)
                Log.d("AlarmScreen", "Time passed, scheduling for tomorrow")
            }
        }
        
        // Calculate time difference for logging
        val timeDiffMillis = calendar.timeInMillis - now.timeInMillis
        val timeDiffMinutes = timeDiffMillis / (1000 * 60)
        val timeDiffHours = timeDiffMinutes / 60
        val remainingMinutes = timeDiffMinutes % 60
        
        // Hiển thị thông báo thời gian dự kiến báo thức sẽ kêu
        val alarmTimeFormat = String.format(
            "Set alarm for: %02d/%02d/%d %02d:%02d (still %d hours %d minutes)",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            timeDiffHours,
            remainingMinutes
        )
        
        Log.d("AlarmScreen", alarmTimeFormat)
        Toast.makeText(context, alarmTimeFormat, Toast.LENGTH_LONG).show()
        
        // Try different alarm setting methods based on API level
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // For API 31+ with exact alarm permission
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d("AlarmScreen", "Alarm set with setExactAndAllowWhileIdle API 31+")
                } else {
                    // Fallback if can't schedule exact alarms
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    Log.d("AlarmScreen", "Alarm set with setAlarmClock API 31+")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For API 23-30
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScreen", "Alarm set with setExactAndAllowWhileIdle API 23-30")
            } else {
                // For API 22 and below
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScreen", "Alarm set with setExact API <23")
            }
            
            // Additional backup alarm using setAlarmClock which shows in system UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    calendar.timeInMillis,
                    pendingIntent
                )
                try {
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    Log.d("AlarmScreen", "Backup alarm set with setAlarmClock")
                } catch (e: Exception) {
                    Log.e("AlarmScreen", "Error setting backup alarm", e)
                }
            }
            
            Toast.makeText(context, "Alarm has been set successfully!", Toast.LENGTH_SHORT).show()
            Log.d("AlarmScreen", "Alarm successfully scheduled for ${calendar.time}")
            
        } catch (e: Exception) {
            Log.e("AlarmScreen", "Error setting alarm", e)
            Toast.makeText(
                context, 
                "Error setting alarm: ${e.message}. Try another method...",
                Toast.LENGTH_LONG
            ).show()
            
            // Fallback method if other methods fail
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d("AlarmScreen", "Fallback alarm set with set()")
                Toast.makeText(context, "Alarm set with fallback method", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Log.e("AlarmScreen", "Critical error: Cannot set alarm. Try restarting the device.", ex)
                Toast.makeText(
                    context,
                    "Critical error: Cannot set alarm. Try restarting the device.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        Log.e("AlarmScreen", "Critical error in setAlarm", e)
        Toast.makeText(
            context,
            "Critical error when setting alarm: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun cancelAlarm(context: Context, id: Int) {
    try {
        Log.d("AlarmScreen", "Cancelling alarm id=$id")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("ALARM_ID", id)
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Toast.makeText(context, "Alarm canceled", Toast.LENGTH_SHORT).show()
        Log.d("AlarmScreen", "Alarm cancelled successfully")
    } catch (e: Exception) {
        Log.e("AlarmScreen", "Error cancelling alarm", e)
        Toast.makeText(context, "Error canceling alarm: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { 
                if (value < range.last) {
                    onValueChange(value + 1)
                }
            }
        ) {
            Text("▲", style = MaterialTheme.typography.titleMedium)
        }
        
        Text(
            text = String.format("%02d", value),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        IconButton(
            onClick = { 
                if (value > range.first) {
                    onValueChange(value - 1)
                }
            }
        ) {
            Text("▼", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(0) }
    var selectedMinute by remember { mutableStateOf(0) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select time",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Hour picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.bodyMedium)
                        NumberPicker(
                            value = selectedHour,
                            onValueChange = { selectedHour = it },
                            range = 0..23
                        )
                    }
                    
                    // Minute picker
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.bodyMedium)
                        NumberPicker(
                            value = selectedMinute,
                            onValueChange = { selectedMinute = it },
                            range = 0..59
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
} 