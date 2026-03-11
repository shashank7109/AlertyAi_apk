package com.alertyai.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * BroadcastReceiver that registers the TaskWidget as an AppWidget provider.
 * Declared in AndroidManifest.xml with APPWIDGET_UPDATE intent-filter.
 */
class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()

    companion object {
        /** Intent actions used to communicate from widget buttons → app */
        const val ACTION_ADD_TASK         = "com.alertyai.app.ACTION_ADD_TASK"
        const val ACTION_WIDGET_VOICE     = "com.alertyai.app.ACTION_WIDGET_VOICE"
        const val ACTION_WIDGET_IMAGE     = "com.alertyai.app.ACTION_WIDGET_IMAGE"
        const val ACTION_TOGGLE_NOTIF     = "com.alertyai.app.ACTION_TOGGLE_NOTIF"
    }
}
