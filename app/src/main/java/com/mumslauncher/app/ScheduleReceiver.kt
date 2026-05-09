package com.mumslauncher.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCHEDULE_CHECK,
            Intent.ACTION_BOOT_COMPLETED,
            -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SchedulePromptController.handleReceiver(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
