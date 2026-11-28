package com.example.doan3.ui.profile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("PREF_NOTIFICATIONS", true)) {
                ReminderScheduler.scheduleDailyReminder(context)
            }
        }
    }
}
