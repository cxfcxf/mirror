package com.cxfcxf.mirror

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.cxfcxf.mirror.service.AirPlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(Prefs.BOOT_AUTO_START, Prefs.DEF_BOOT_AUTO_START)) return

        val serviceIntent = Intent(context, AirPlayService::class.java)
            .setAction(AirPlayService.ACTION_START_SERVER)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
