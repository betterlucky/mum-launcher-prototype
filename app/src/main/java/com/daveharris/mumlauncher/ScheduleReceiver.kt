package com.daveharris.mumlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCHEDULE_CHECK,
            Intent.ACTION_BOOT_COMPLETED,
            -> SchedulePromptController.handleReceiver(context)
        }
    }
}
