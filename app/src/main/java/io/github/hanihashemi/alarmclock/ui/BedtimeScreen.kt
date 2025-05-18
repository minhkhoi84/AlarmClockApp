package io.github.hanihashemi.alarmclock.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen() {
    val context = LocalContext.current
    var noteText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(listOf<Note>()) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

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
        Text("Ghi chú/Lập lịch", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = { Text("Nhập ghi chú mới") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = { showDatePicker() }) {
                Text(if (selectedDate.isEmpty()) "Chọn ngày" else selectedDate)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showTimePicker() }) {
                Text(if (selectedTime.isEmpty()) "Chọn giờ" else selectedTime)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (noteText.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()) {
                    notes = notes + Note(noteText, selectedDate, selectedTime)
                    noteText = ""
                    selectedDate = ""
                    selectedTime = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Lưu")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Danh sách ghi chú:", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(notes) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(note.text)
                        Text("Ngày: ${note.date}  Giờ: ${note.time}", style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                notes = notes - note
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa ghi chú")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Note(
    val text: String,
    val date: String, // ví dụ: "2024-05-18"
    val time: String  // ví dụ: "14:30"
) 