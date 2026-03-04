package com.alertyai.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.alertyai.app.data.model.Task

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    /**
     * Schedule an exact alarm for a task if:
     *  1. alarmEnabled is true
     *  2. dueDate + dueTime are both set
     *  The alarm fires at (dueDate + dueTime - remindMinsBefore)
     */
    fun schedule(context: Context, task: Task) {
        if (!task.alarmEnabled) return
        val dueDate = task.dueDate ?: return
        val dueTime = task.dueTime ?: return

        // Correctly combine date and time.
        // dueDate is a timestamp for day @ 00:00:00
        // dueTime is a timestamp for some day @ HH:mm:00
        // We extract HH:mm from dueTime and apply it to dueDate.
        val dateCal = java.util.Calendar.getInstance().apply { timeInMillis = dueDate }
        val timeCal = java.util.Calendar.getInstance().apply { timeInMillis = dueTime }

        val triggerCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, dateCal.get(java.util.Calendar.YEAR))
            set(java.util.Calendar.MONTH, dateCal.get(java.util.Calendar.MONTH))
            set(java.util.Calendar.DAY_OF_MONTH, dateCal.get(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, timeCal.get(java.util.Calendar.HOUR_OF_DAY))
            set(java.util.Calendar.MINUTE, timeCal.get(java.util.Calendar.MINUTE))
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val triggerAtMillis = triggerCal.timeInMillis - (task.remindMinsBefore * 60 * 1000L)

        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Alarm time is in the past for task: ${task.title} (at $triggerAtMillis), skipping.")
            return
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if exact alarm permission is granted (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Log.w(TAG, "No SCHEDULE_EXACT_ALARM permission — using inexact alarm")
            }
        }

        val reminderLabel = when (task.remindMinsBefore) {
            0    -> "It's time!"
            10   -> "10 minutes remaining"
            30   -> "30 minutes remaining"
            60   -> "1 hour remaining"
            else -> "${task.remindMinsBefore} minutes remaining"
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.alertyai.ALARM_ACTION"
            putExtra(AlarmReceiver.EXTRA_TITLE, "⏰ ${task.title}")
            putExtra(AlarmReceiver.EXTRA_BODY, reminderLabel)
            putExtra(AlarmReceiver.EXTRA_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.i(TAG, "✅ Alarm set for '${task.title}' at $triggerAtMillis (remind ${task.remindMinsBefore}m before)")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling alarm: ${e.message}")
        }
    }

    /** Cancel a previously scheduled alarm for a task. */
    fun cancel(context: Context, task: Task) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.alertyai.ALARM_ACTION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            am.cancel(pendingIntent)
            Log.i(TAG, "🗑️ Alarm cancelled for '${task.title}'")
        }
    }
}
