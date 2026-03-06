package com.alertyai.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.alertyai.app.MainActivity
import com.alertyai.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID   = "alertyai_reminders"
        const val CHANNEL_NAME = "AlertyAI Reminders"
        const val EXTRA_TITLE  = "extra_title"
        const val EXTRA_BODY   = "extra_body"
        const val EXTRA_ID     = "extra_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // On device boot — re-schedule all alarms from Room DB
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val tasks = db.taskDao().getTasksWithAlarms()
                    tasks.forEach { AlarmScheduler.schedule(context, it) }
                    Log.i("AlarmReceiver", "📲 Re-scheduled ${tasks.size} alarms after boot")
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Actual alarm fired — show notification
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val body  = intent.getStringExtra(EXTRA_BODY)  ?: ""
        val id    = intent.getIntExtra(EXTRA_ID, 0)

        createChannel(context)

        val tapIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(tapIntent)
            
        val loudAlarmEnabled = context.getSharedPreferences("alertyai_prefs", Context.MODE_PRIVATE).getBoolean("loud_alarm", false)
        if (loudAlarmEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        } else {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        val notification = builder.build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled task and reminder alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
