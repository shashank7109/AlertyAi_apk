package com.alertyai.app.util

import android.content.Context
import android.content.Intent
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

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, "⏰ ${task.title}")
            putExtra(AlarmClock.EXTRA_HOUR, timeCal.get(Calendar.HOUR_OF_DAY))
            putExtra(AlarmClock.EXTRA_MINUTES, timeCal.get(Calendar.MINUTE))
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
            Log.i(TAG, "✅ System Alarm set for '${task.title}' at ${timeCal.get(Calendar.HOUR_OF_DAY)}:${timeCal.get(Calendar.MINUTE)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch system alarm: ${e.message}")
        }
    }

    /** Cancel a previously scheduled alarm. */
    fun cancel(context: Context, task: Task) {
        // System alarms cannot be reliably cancelled without UI, so we log and ignore
        Log.i(TAG, "System alarms cannot be silently cancelled. Cancel manually in Clock app.")
    }
}
