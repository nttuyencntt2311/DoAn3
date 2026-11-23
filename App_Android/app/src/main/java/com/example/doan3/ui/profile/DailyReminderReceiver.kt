package com.example.doan3.ui.profile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.doan3.R

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "lexivt_daily_reminder"
        const val NOTIFICATION_ID = 2026
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("PREF_NOTIFICATIONS", true)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc học LexiVT hằng ngày",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nhắc nhở ôn tập từ vựng và giữ chuỗi Streak lúc 20:00 mỗi ngày"
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, dangnhap::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val goalMins = prefs.getInt("DAILY_GOAL_MINUTES", 10)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fire)
            .setContentTitle("🔥 Đừng để đứt chuỗi Streak LexiVT!")
            .setContentText("Chỉ $goalMins phút ôn từ vựng cùng AI hôm nay để giữ vững phong độ nhé.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Người dùng chưa cấp quyền POST_NOTIFICATIONS trên Android 13+
        }
    }
}
