package com.alertyai.app.widget

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.alertyai.app.MainActivity

class QuickSettingsVoiceTileService : TileService() {

    companion object {
        const val ACTION_QUICK_SETTINGS_VOICE = "com.alertyai.app.ACTION_QUICK_SETTINGS_VOICE"
    }

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile
        tile?.state = Tile.STATE_INACTIVE
        tile?.updateTile()
    }

    override fun onClick() {
        super.onClick()
        
        // Launch MainActivity with a specific action
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_QUICK_SETTINGS_VOICE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        // Use startActivityAndCollapse to launch the app and close the notification shade
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
