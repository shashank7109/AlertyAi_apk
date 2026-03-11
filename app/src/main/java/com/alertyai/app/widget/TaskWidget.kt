package com.alertyai.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.alertyai.app.MainActivity

/**
 * Home screen widget — three action buttons: Voice, Scan (Alerts), New Task
 * Designed to match the light theme: White rounded cards, gray circular icon backgrounds.
 */
class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content(context) }
    }

    @Composable
    private fun Content(context: Context) {
        // Main container (transparent so cards float)
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                Spacer(GlanceModifier.defaultWeight())

                // ── [Voice] Button ──────────────────────────────────────
                WidgetCard(
                    context = context,
                    iconResId = android.R.drawable.ic_btn_speak_now,
                    label = "Voice",
                    onClickIntent = Intent(context, VoiceWidgetReceiver::class.java).apply {
                        action = TaskWidgetReceiver.ACTION_WIDGET_VOICE
                    },
                    isBroadcast = true
                )

                Spacer(GlanceModifier.width(16.dp))

                // ── [Scan/Alerts] Button ──────────────────────────────────
                WidgetCard(
                    context = context,
                    iconResId = android.R.drawable.ic_menu_camera,  // Placeholder for scan/camera
                    label = "Scan",
                    onClickIntent = Intent(context, MainActivity::class.java).apply {
                        action = TaskWidgetReceiver.ACTION_WIDGET_IMAGE
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    isBroadcast = false
                )

                Spacer(GlanceModifier.width(16.dp))

                // ── [New Task] Button ────────────────────────────────────
                WidgetCard(
                    context = context,
                    iconResId = android.R.drawable.ic_input_add,
                    label = "New",
                    onClickIntent = Intent(context, MainActivity::class.java).apply {
                        action = TaskWidgetReceiver.ACTION_ADD_TASK
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    isBroadcast = false
                )
                
                Spacer(GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
private fun WidgetCard(
    context: Context,
    iconResId: Int,
    label: String,
    onClickIntent: Intent,
    isBroadcast: Boolean
) {
    val clickModifier = if (isBroadcast) {
        GlanceModifier.clickable(actionSendBroadcast(onClickIntent))
    } else {
        GlanceModifier.clickable(actionStartActivity(onClickIntent))
    }

    Box(
        modifier = GlanceModifier
            .size(72.dp)
            .background(Color(0xFFFFFFFF)) // White card
            .cornerRadius(16.dp)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.padding(top = 10.dp)
        ) {
            // Gray circle for icon
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(Color(0xFFF3F4F6)) // Light gray circle
                    .cornerRadius(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(iconResId),
                    contentDescription = label,
                    modifier = GlanceModifier.size(20.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF111827))) // Dark gray icon
                )
            }
            
            Spacer(GlanceModifier.height(6.dp))
            
            // Text label
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorProvider(Color(0xFF111827)) // Dark gray text
                )
            )
        }
    }
}
