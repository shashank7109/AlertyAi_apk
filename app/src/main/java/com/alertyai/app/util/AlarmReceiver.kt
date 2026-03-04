package com.alertyai.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.alertyai.app.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID   = "alertyai_reminders"
        const val EXTRA_TITLE  = "extra_title"
        const val EXTRA_BODY   = "extra_body"
        const val EXTRA_ID     = "extra_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val body  = intent.getStringExtra(EXTRA_BODY)  ?: ""
        val id    = intent.getIntExtra(EXTRA_ID, 0)

        createChannel(context)

        val tapIntent = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "AlertyAI Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Scheduled task and reminder alerts" }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
