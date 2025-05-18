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
import android.app.Service
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.hanihashemi.alarmclock.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.IOException

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "AlarmChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP_ALARM = "io.github.hanihashemi.alarmclock.STOP_ALARM"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d(TAG, "AlarmReceiver.onReceive at $currentTime, action: ${intent.action}")
        
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
        
        Toast.makeText(context, "Báo thức đã kích hoạt lúc $currentTime!", Toast.LENGTH_LONG).show()
        Log.d(TAG, "Alarm triggered at $currentTime")
        
        // Acquire wake lock to ensure device stays awake for alarm
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "AlarmClock:AlarmWakeLock"
        )
        wakeLock.acquire(120000) // 2 minute timeout
        
        // Khởi động dịch vụ báo thức để đảm bảo âm thanh phát kể cả khi app bị kill
        val serviceIntent = Intent(context, AlarmService::class.java)
        serviceIntent.putExtra("ALARM_ID", intent.getIntExtra("ALARM_ID", 0))
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
        createNotification(context)
        
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
    
    private fun createNotification(context: Context) {
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
            .setContentTitle("Báo thức")
            .setContentText("Báo thức đang kêu. Nhấp để tắt.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tắt", stopPendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        notification.flags = notification.flags or Notification.FLAG_INSISTENT or Notification.FLAG_NO_CLEAR
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notification displayed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }
}

/**
 * Dịch vụ chạy nền để đảm bảo báo thức vẫn phát âm thanh ngay cả khi ứng dụng bị đóng
 */
class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
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
        wakeLock?.acquire(10*60*1000L) // 10 minutes timeout
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
            // Đặt âm lượng tối đa
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            
            // Đảm bảo không tắt tiếng
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_ALARM,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            }
            
            // Sử dụng âm thanh hệ thống thay vì file âm thanh tùy chỉnh
            try {
                // Lấy âm thanh báo thức mặc định của hệ thống
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                
                // Nếu không có âm thanh báo thức, thử dùng âm báo thông báo
                val notificationUri = if (alarmUri == null) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else {
                    alarmUri
                }
                
                // Nếu vẫn không có, dùng nhạc chuông
                val uriToUse = notificationUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                
                Log.d(TAG, "Using system alarm sound: $uriToUse")
                
                // Stop and release existing MediaPlayer if it exists
                mediaPlayer?.apply {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                }
                
                // Tạo MediaPlayer mới
                mediaPlayer = MediaPlayer().apply {
                    // Đặt thuộc tính âm thanh
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                    } else {
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                    }
                    
                    // Thiết lập nguồn dữ liệu từ hệ thống
                    setDataSource(applicationContext, uriToUse)
                    prepare()
                    isLooping = true
                    
                    // Set volume to maximum
                    setVolume(1.0f, 1.0f)
                    
                    start()
                    
                    Log.d(TAG, "System alarm sound playing successfully")
                }
                
                // Lưu MediaPlayer trong Application để có thể dừng từ các thành phần khác
                (applicationContext as? AlarmClockApplication)?.setMediaPlayer(mediaPlayer)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error playing system alarm sound", e)
                
                // Phương pháp backup - thử dùng âm thanh notification hoặc ringtone nếu alarm fail
                try {
                    val backupUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    
                    // Stop and release existing MediaPlayer if it exists
                    mediaPlayer?.apply {
                        if (isPlaying) {
                            stop()
                        }
                        release()
                    }
                    
                    mediaPlayer = MediaPlayer().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                        } else {
                            setAudioStreamType(AudioManager.STREAM_ALARM)
                        }
                        
                        setDataSource(applicationContext, backupUri)
                        prepare()
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        start()
                        
                        Log.d(TAG, "Backup system sound playing successfully")
                    }
                    
                    (applicationContext as? AlarmClockApplication)?.setMediaPlayer(mediaPlayer)
                } catch (ex: Exception) {
                    Log.e(TAG, "Error playing backup system sound", ex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up alarm sound", e)
        }
    }
    
    private fun stopAlarmSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "Alarm sound stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm sound", e)
        }
    }
} 