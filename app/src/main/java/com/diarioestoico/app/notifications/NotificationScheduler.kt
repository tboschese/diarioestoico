package com.diarioestoico.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.diarioestoico.app.MainActivity
import com.diarioestoico.app.R
import com.diarioestoico.app.data.EntryRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val CHANNEL_ID   = "diario_estoico_daily"
const val WORK_NAME    = "daily_reading_notification"

fun createNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Leitura Diária",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Lembrete diário para a meditação estoica"
    }
    context.getSystemService(NotificationManager::class.java)
        .createNotificationChannel(channel)
}

fun scheduleNotification(context: Context, hour: Int, minute: Int) {
    val initialDelay = millisUntil(hour, minute)

    val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder().build())
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun cancelNotification(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
}

fun showDailyNotification(context: Context) {
    val entry = EntryRepository(context).getTodayEntry() ?: return

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stoic_splash)
        .setContentTitle(entry.title)
        .setContentText(entry.widgetQuote)
        .setStyle(NotificationCompat.BigTextStyle().bigText(entry.widgetQuote))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    context.getSystemService(NotificationManager::class.java)
        .notify(1, notification)
}

// Returns milliseconds until the next occurrence of hour:minute
fun millisUntil(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis - now.timeInMillis
}
