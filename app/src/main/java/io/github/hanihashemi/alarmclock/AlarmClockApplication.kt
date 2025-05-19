package io.github.hanihashemi.alarmclock

import android.app.Application
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

class AlarmClockApplication : Application() {
    private var _mediaPlayer: MediaPlayer? = null
    private val TAG = "AlarmClockApplication"
    
    // Make mediaPlayer accessible for UI (read-only)
    val mediaPlayer: MediaPlayer? 
        get() = _mediaPlayer
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "AlarmClockApplication initialized")
    }
    
    fun setMediaPlayer(player: MediaPlayer?) {
        // Dọn dẹp mediaPlayer cũ nếu tồn tại
        stopAlarm()
        
        _mediaPlayer = player
        Log.d(TAG, "New MediaPlayer set: ${player != null}")
    }
    
    fun clearMediaPlayer() {
        _mediaPlayer = null
        Log.d(TAG, "MediaPlayer reference cleared")
    }
    
    fun stopAlarm() {
        try {
            _mediaPlayer?.apply {
                if (isPlaying) {
                    Log.d(TAG, "Stopping playing alarm sound")
                    stop()
                }
                Log.d(TAG, "Releasing MediaPlayer resources")
                release()
            }
            _mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm", e)
        }
    }
    
    override fun onTerminate() {
        stopAlarm()
        super.onTerminate()
    }
    
    companion object {
        lateinit var instance: AlarmClockApplication
            private set
    }
} 