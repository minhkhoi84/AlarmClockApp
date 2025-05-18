package io.github.hanihashemi.alarmclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import io.github.hanihashemi.alarmclock.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.nhac_chuong_bao_thuc_iphone_12_www_tiengdong_com)
        mediaPlayer.start()
    }
} 