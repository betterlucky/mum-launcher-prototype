package com.daveharris.mumlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daveharris.mumlauncher.data.SettingsStore
import kotlinx.coroutines.runBlocking

class KioskRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runBlocking {
            SettingsStore(context).markSystemEvent(System.currentTimeMillis())
        }
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            ACTION_SCHEDULE_CHECK,
            -> SchedulePromptController.handleReceiver(context)
        }
    }
}
