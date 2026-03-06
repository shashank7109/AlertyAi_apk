package com.alertyai.app.util

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.provider.AlarmClock
import android.util.Log
import com.alertyai.app.data.model.Task
import java.util.Calendar

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Set a system alarm using the default Clock app.
     */
    fun schedule(context: Context, task: Task) {
        if (!task.alarmEnabled) return
        val dueDate = task.dueDate ?: return
        val dueTime = task.dueTime ?: return

        val timeCal = Calendar.getInstance().apply { timeInMillis = dueTime }
        
        // Don't schedule in the past
        if (timeCal.timeInMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.alertyai.ALARM_ACTION"
            putExtra(AlarmReceiver.EXTRA_TITLE, "Task Due: ${task.title}")
            putExtra(AlarmReceiver.EXTRA_BODY, task.note.ifBlank { "It's time for your scheduled task." })
            putExtra(AlarmReceiver.EXTRA_ID, task.id.hashCode())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                timeCal.timeInMillis,
                pendingIntent
            )
            Log.i(TAG, "✅ App Alarm scheduled for '${task.title}' at ${timeCal.time}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarms permission missing: ${e.message}")
        }
    }

    /** Cancel a previously scheduled alarm. */
    fun cancel(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.alertyai.ALARM_ACTION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Cancelled alarm for '${task.title}'")
    }
}
