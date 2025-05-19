package io.github.hanihashemi.alarmclock.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.hanihashemi.alarmclock.AlarmReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BedtimeScreen() {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(listOf<Note>()) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var enableReminder by remember { mutableStateOf(true) }
    
    // Load notes data when screen is initialized
    LaunchedEffect(Unit) {
        loadNotesFromStorage(context)?.let {
            notes = it
        }
    }

    // DatePickerDialog
    fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // TimePickerDialog
    fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedTime = "%02d:%02d".format(hour, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text("Notes/Schedule", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Enter new note") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = { showDatePicker() }) {
                Text(if (selectedDate.isEmpty()) "Select date" else selectedDate)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showTimePicker() }) {
                Text(if (selectedTime.isEmpty()) "Select time" else selectedTime)
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Set reminder alarm:", modifier = Modifier.weight(1f))
            Switch(
                checked = enableReminder,
                onCheckedChange = { enableReminder = it }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (noteText.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()) {
                    val newId = (notes.maxOfOrNull { it.id } ?: 0) + 1
                    val newNote = Note(
                        id = newId,
                        text = noteText, 
                        date = selectedDate, 
                        time = selectedTime,
                        hasReminder = enableReminder
                    )
                    val updatedNotes = notes + newNote
                    notes = updatedNotes
                    
                    // Save new notes list
                    saveNotesToStorage(context, updatedNotes)
                    
                    // Set alarm if enabled
                    if (enableReminder) {
                        scheduleNoteReminder(context, newNote)
                    }
                    
                    noteText = ""
                    selectedDate = ""
                    selectedTime = ""
                    Toast.makeText(
                        context, 
                        if (enableReminder) "Note saved and reminder set" else "Note saved", 
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(context, "Please enter content, date and time", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Notes list:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(note.text)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Date: ${note.date}  Time: ${note.time}", 
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            // Hiển thị biểu tượng báo thức
                            if (note.hasReminder) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Has reminder",
                                    tint = Color(0xFF90CAF9),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "No reminder",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            // Nút bật/tắt báo thức
                            IconButton(onClick = {
                                val updatedNotes = notes.map {
                                    if (it.id == note.id) {
                                        val updatedNote = it.copy(hasReminder = !it.hasReminder)
                                        if (updatedNote.hasReminder) {
                                            // Đặt báo thức nếu bật
                                            scheduleNoteReminder(context, updatedNote)
                                            Toast.makeText(context, "Reminder enabled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Hủy báo thức nếu tắt
                                            cancelNoteReminder(context, updatedNote.id)
                                            Toast.makeText(context, "Reminder disabled", Toast.LENGTH_SHORT).show()
                                        }
                                        updatedNote
                                    } else it
                                }
                                notes = updatedNotes
                                // Lưu trạng thái mới
                                saveNotesToStorage(context, updatedNotes)
                            }) {
                                Icon(
                                    imageVector = if (note.hasReminder) Icons.Default.Notifications else Icons.Default.Info,
                                    contentDescription = if (note.hasReminder) "Disable reminder" else "Enable reminder",
                                    tint = if (note.hasReminder) Color(0xFF90CAF9) else Color.Gray
                                )
                            }
                            
                            // Nút xóa
                            IconButton(onClick = {
                                // Hủy báo thức khi xóa ghi chú
                                if (note.hasReminder) {
                                    cancelNoteReminder(context, note.id)
                                }
                                val updatedNotes = notes - note
                                notes = updatedNotes
                                // Lưu danh sách sau khi xóa
                                saveNotesToStorage(context, updatedNotes)
                                Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Delete note"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Note(
    val id: Int,
    val text: String,
    val date: String, // example: "2024-05-18"
    val time: String,  // example: "14:30"
    val hasReminder: Boolean = false
)

// Save notes list to SharedPreferences
fun saveNotesToStorage(context: Context, notes: List<Note>) {
    try {
        val sharedPrefs = context.getSharedPreferences("AlarmClockNotes", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        
        val jsonArray = JSONArray()
        for (note in notes) {
            val jsonObject = JSONObject().apply {
                put("id", note.id)
                put("text", note.text)
                put("date", note.date)
                put("time", note.time)
                put("hasReminder", note.hasReminder)
            }
            jsonArray.put(jsonObject)
        }
        
        editor.putString("notes", jsonArray.toString())
        editor.apply()
        
        Log.d("BedtimeScreen", "Saved ${notes.size} notes")
    } catch (e: Exception) {
        Log.e("BedtimeScreen", "Error saving notes: ${e.message}")
    }
}

// Load notes list from SharedPreferences
fun loadNotesFromStorage(context: Context): List<Note>? {
    try {
        val sharedPrefs = context.getSharedPreferences("AlarmClockNotes", Context.MODE_PRIVATE)
        val notesJson = sharedPrefs.getString("notes", null) ?: return emptyList()
        
        val jsonArray = JSONArray(notesJson)
        val notesList = mutableListOf<Note>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val note = Note(
                id = jsonObject.getInt("id"),
                text = jsonObject.getString("text"),
                date = jsonObject.getString("date"),
                time = jsonObject.getString("time"),
                hasReminder = jsonObject.getBoolean("hasReminder")
            )
            notesList.add(note)
        }
        
        Log.d("BedtimeScreen", "Loaded ${notesList.size} notes")
        
        // Check notes for expired times and remove expired alarms
        checkAndRescheduleReminders(context, notesList)
        
        return notesList
    } catch (e: Exception) {
        Log.e("BedtimeScreen", "Error loading notes: ${e.message}")
        return emptyList()
    }
}

// Kiểm tra và cập nhật lại các báo thức
fun checkAndRescheduleReminders(context: Context, notes: List<Note>) {
    val now = Calendar.getInstance()
    
    for (note in notes) {
        if (note.hasReminder) {
            try {
                val dateParts = note.date.split("-")
                val timeParts = note.time.split(":")
                
                if (dateParts.size != 3 || timeParts.size != 2) continue
                
                val year = dateParts[0].toInt()
                val month = dateParts[1].toInt() - 1
                val day = dateParts[2].toInt()
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                
                val noteTime = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                // If note time is still in the future, reset the alarm
                if (noteTime.after(now)) {
                    Log.d("BedtimeScreen", "Reset alarm for note ID ${note.id}")
                    scheduleNoteReminder(context, note)
                }
            } catch (e: Exception) {
                Log.e("BedtimeScreen", "Error checking note ${note.id}: ${e.message}")
            }
        }
    }
}

// Function to schedule alarm for note
fun scheduleNoteReminder(context: Context, note: Note) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            Log.e("BedtimeScreen", "Cannot create alarm, AlarmManager null")
            Toast.makeText(context, "Cannot create alarm, AlarmManager null", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create Calendar from note date and time
        val dateParts = note.date.split("-")
        val timeParts = note.time.split(":")
        
        if (dateParts.size != 3 || timeParts.size != 2) {
            Log.e("BedtimeScreen", "Invalid date/time format")
            return
        }
        
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Month in Calendar is 0-11
        val day = dateParts[2].toInt()
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Check if time has passed
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            Toast.makeText(context, "Time has passed, cannot set reminder", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Create Intent for AlarmReceiver
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", note.id)
            putExtra("IS_NOTE", true) // Mark this as a note alarm
            putExtra("NOTE_TEXT", note.text) // Note content
            flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        }
        
        // Create PendingIntent
        // Use different ID from regular alarms by adding prefix
        val noteRequestCode = 1000000 + note.id
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Set alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            // Format readable time
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val formattedDateTime = dateFormat.format(calendar.time)
            
            Log.d("BedtimeScreen", "Set reminder for note ID ${note.id} at $formattedDateTime")
            
        } catch (e: Exception) {
            Log.e("BedtimeScreen", "Error setting alarm: ${e.message}")
            Toast.makeText(context, "Cannot set reminder: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        
    } catch (e: Exception) {
        Log.e("BedtimeScreen", "Critical error: ${e.message}")
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Function to cancel alarm for note
fun cancelNoteReminder(context: Context, noteId: Int) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("ALARM_ID", noteId)
        intent.putExtra("IS_NOTE", true)
        
        val noteRequestCode = 1000000 + noteId
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            noteRequestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        
        Log.d("BedtimeScreen", "Cancelled reminder for note ID $noteId")
        
    } catch (e: Exception) {
        Log.e("BedtimeScreen", "Error cancelling reminder: ${e.message}")
    }
} 