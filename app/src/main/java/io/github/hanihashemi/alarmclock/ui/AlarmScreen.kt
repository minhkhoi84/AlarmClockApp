package io.github.hanihashemi.alarmclock.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hanihashemi.alarmclock.AlarmReceiver
import java.util.*
import android.app.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen() {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(listOf<AlarmItem>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempHour by remember { mutableStateOf(6) }
    var tempMinute by remember { mutableStateOf(0) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }

    if (showTimePicker) {
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
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
            },
            tempHour,
            tempMinute,
            false
        ).apply {
            setOnCancelListener {
                showTimePicker = false
                editingAlarm = null
            }
        }.show()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Báo thức", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(alarms) { alarm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            editingAlarm = alarm
                            tempHour = alarm.hour
                            tempMinute = alarm.minute
                            showTimePicker = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Chỉnh sửa báo thức"
                            )
                        }
                        IconButton(onClick = {
                            alarms = alarms.filter { it.id != alarm.id }
                            cancelAlarm(context, alarm.id)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa báo thức"
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
                }
            }
        }
        Button(
            onClick = {
                editingAlarm = null
                tempHour = 6
                tempMinute = 0
                showTimePicker = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Thêm báo thức mới")
        }
    }
}

@Immutable
data class AlarmItem(val id: Int, val hour: Int, val minute: Int, val enabled: Boolean)

fun setAlarm(context: Context, hour: Int, minute: Int, id: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DATE, 1)
        }
    }
    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
    )
}

fun cancelAlarm(context: Context, id: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
} 