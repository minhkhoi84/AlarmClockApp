package io.github.hanihashemi.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.PowerManager
import android.widget.Toast
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.hanihashemi.alarmclock.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "AlarmChannel"
        const val NOTE_CHANNEL_ID = "NoteReminderChannel"
        const val NOTIFICATION_ID = 1
        const val NOTE_NOTIFICATION_ID_BASE = 2000
        const val ACTION_STOP_ALARM = "io.github.hanihashemi.alarmclock.STOP_ALARM"
        const val ACTION_DISMISS_NOTE = "io.github.hanihashemi.alarmclock.DISMISS_NOTE"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d(TAG, "AlarmReceiver.onReceive at $currentTime, action: ${intent.action}")
        
        // Kiểm tra nếu là lệnh dừng báo thức
        if (intent.action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Stopping alarm")
            (context.applicationContext as? AlarmClockApplication)?.stopAlarm()
            
            // Dừng cả dịch vụ nếu đang chạy
            context.stopService(Intent(context, AlarmService::class.java))
            
            // Cancel notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            
            Toast.makeText(context, "Đã tắt báo thức", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Kiểm tra nếu là lệnh bỏ qua thông báo ghi chú
        if (intent.action == ACTION_DISMISS_NOTE) {
            val noteId = intent.getIntExtra("NOTE_ID", -1)
            if (noteId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTE_NOTIFICATION_ID_BASE + noteId)
                Log.d(TAG, "Dismissed note reminder notification for ID: $noteId")
            }
            return
        }
        
        // Kiểm tra nếu là thông báo ghi chú hay báo thức thông thường
        val isNote = intent.getBooleanExtra("IS_NOTE", false)
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        
        Log.d(TAG, "Alarm triggered: isNote=$isNote, alarmId=$alarmId")
        
        if (isNote) {
            // Xử lý thông báo nhắc nhở ghi chú
            val noteText = intent.getStringExtra("NOTE_TEXT") ?: "Reminder note"
            Toast.makeText(context, "Reminder: $noteText", Toast.LENGTH_LONG).show()
            
            // Tạo thông báo cho ghi chú
            createNoteNotification(context, alarmId, noteText)
            
        } else {
            // Xử lý báo thức thông thường
            Toast.makeText(context, "Alarm activated at $currentTime!", Toast.LENGTH_LONG).show()
            
            // Acquire wake lock to ensure device stays awake for alarm
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "AlarmClock::AlarmWakeLock"
            )
            wakeLock.acquire(300000) // 5 minutes timeout (increased from 2 minutes)
            
            // Khởi động dịch vụ báo thức để đảm bảo âm thanh phát kể cả khi app bị kill
            val serviceIntent = Intent(context, AlarmService::class.java)
            serviceIntent.putExtra("ALARM_ID", alarmId)
            serviceIntent.putExtra("WAKE_DEVICE", true)
            serviceIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            // Thử mở activity chính để đảm bảo ứng dụng được kích hoạt
            try {
                val launchIntent = Intent(context, MainActivity::class.java)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                launchIntent.putExtra("ALARM_TRIGGERED", true)
                context.startActivity(launchIntent)
                Log.d(TAG, "Started main activity")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start main activity", e)
            }
            
            // Create notification
            createAlarmNotification(context)
            
            // Release wake lock after everything is set up
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "WakeLock released")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wake lock", e)
            }
        }
    }
    
    private fun createNoteNotification(context: Context, noteId: Int, noteText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTE_CHANNEL_ID,
                "Reminder notes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled notes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Intent để mở ứng dụng tại màn hình ghi chú khi nhấn thông báo
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        contentIntent.putExtra("SCREEN_INDEX", 4) // Switch to Bedtime/Note screen
        val pendingContentIntent = PendingIntent.getActivity(
            context, noteId, contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent để bỏ qua thông báo
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTE
            putExtra("NOTE_ID", noteId)
        }
        val pendingDismissIntent = PendingIntent.getBroadcast(
            context, noteId, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, NOTE_CHANNEL_ID)
            .setContentTitle("Note reminder")
            .setContentText(noteText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(noteText))
            .setSmallIcon(R.drawable.ic_outline_hotel_24) // Use bed icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", pendingDismissIntent)
            .build()
        
        try {
            // Sử dụng ID dựa trên ID của ghi chú để tránh xung đột
            notificationManager.notify(NOTE_NOTIFICATION_ID_BASE + noteId, notification)
            Log.d(TAG, "Note notification displayed successfully for ID: $noteId")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing note notification", e)
        }
    }
    
    private fun createAlarmNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alarm clock"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(null, null) // Disable channel sound as we're handling it manually
                setBypassDnd(true) // Bypass Do Not Disturb mode
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Intent to open app when notification is clicked
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        contentIntent.putExtra("ALARM_TRIGGERED", true)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create stop alarm intent using the same receiver
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Alarm")
            .setContentText("Alarm is ringing. Tap to turn off.")
            .setSmallIcon(R.drawable.ic_outline_alarm_24)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notification.flags = notification.flags or Notification.FLAG_INSISTENT or Notification.FLAG_NO_CLEAR
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Alarm notification displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing alarm notification", e)
        }
    }
} 