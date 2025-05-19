package io.github.hanihashemi.alarmclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException

/**
 * Dịch vụ chạy nền để đảm bảo báo thức vẫn phát âm thanh ngay cả khi ứng dụng bị đóng
 */
class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var noteMediaPlayer: MediaPlayer? = null
    private val TAG = "AlarmService"
    private var wakeLock: PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created")
        
        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlarmClock:AlarmServiceWakeLock"
        )
        wakeLock?.acquire(30*60*1000L) // 30 phút timeout thay vì 10 phút
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "AlarmService started with flags: $flags, startId: $startId")
        
        // Tạo notification để chạy foreground service (bắt buộc trên Android 8+)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AlarmReceiver.CHANNEL_ID,
                "Alarm Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Foreground service for alarms"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(null, null) // Disable channel sound as we're handling it manually
                setBypassDnd(true) // Bypass Do Not Disturb mode
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Intent để mở ứng dụng
        val contentIntent = Intent(this, MainActivity::class.java)
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        contentIntent.putExtra("ALARM_TRIGGERED", true)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Tạo intent để dừng báo thức
        val stopIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Tạo notification
        val notification = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID)
            .setContentTitle("Báo thức")
            .setContentText("Báo thức đang kêu. Nhấp để tắt.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tắt", stopPendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notification.flags = notification.flags or Notification.FLAG_INSISTENT or Notification.FLAG_NO_CLEAR
        
        // Khởi chạy foreground service
        startForeground(AlarmReceiver.NOTIFICATION_ID, notification)
        
        // Phát âm thanh báo thức
        playAlarmSound()
        
        // If the service gets killed, make sure it restarts
        return START_REDELIVER_INTENT
    }
    
    override fun onDestroy() {
        Log.d(TAG, "AlarmService destroyed")
        stopAlarmSound()
        
        // Release wake lock if held
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released in onDestroy")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock in onDestroy", e)
        }
        
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun playAlarmSound() {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                }
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playNoteSound() {
        try {
            if (noteMediaPlayer == null) {
                noteMediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = false
                    prepare()
                }
            }
            noteMediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAlarmSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                try {
                    stop()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error stopping MediaPlayer", e)
                }
            }
            release()
        }
        mediaPlayer = null
        
        // Also clear from Application
        (applicationContext as? AlarmClockApplication)?.clearMediaPlayer()
    }
} 