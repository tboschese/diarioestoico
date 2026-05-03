package com.diarioestoico.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.diarioestoico.app.data.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = NotificationPreferences(context).settings.first()
            if (prefs.enabled) {
                scheduleNotification(context, prefs.hour, prefs.minute)
            }
        }
    }
}
