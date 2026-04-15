package com.daveharris.mumlauncher

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daveharris.mumlauncher.data.LauncherMode
import com.daveharris.mumlauncher.data.LauncherSettings
import com.daveharris.mumlauncher.data.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val SCHEDULE_CHANNEL_ID = "schedule_prompts"
private const val ENTER_NOTIFICATION_ID = 1001
private const val EXIT_NOTIFICATION_ID = 1002
const val ACTION_SCHEDULE_CHECK = "com.daveharris.mumlauncher.action.SCHEDULE_CHECK"
const val EXTRA_PROMPT_ACTION = "prompt_action"

object SchedulePromptController {
    fun sync(context: Context, settings: LauncherSettings) {
        ensureNotificationChannel(context)
        cancelScheduledCheck(context)
        maybeNotifyForCurrentState(context, settings)

        val nextCheckAt = nextScheduleBoundaryMillis(settings, System.currentTimeMillis()) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = schedulePendingIntent(context)
        if (canScheduleExactAlarms(context)) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextCheckAt,
                pendingIntent,
            )
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                nextCheckAt,
                120_000L,
                pendingIntent,
            )
        }
    }

    fun handleReceiver(context: Context) {
        val settings = runBlocking { SettingsStore(context).settings.first() }
        sync(context, settings)
    }

    private fun maybeNotifyForCurrentState(context: Context, settings: LauncherSettings) {
        if (settings.launcherMode != LauncherMode.SCHEDULED || !settings.setupComplete) {
            cancelNotifications(context)
            return
        }
        if (!canPostNotifications(context)) {
            cancelNotifications(context)
            return
        }

        val nowMs = System.currentTimeMillis()
        when (val duePrompt = currentDuePrompt(settings, nowMs)) {
            null -> cancelNotifications(context)
            else -> when (duePrompt.action) {
                PromptAction.ENTER_FOCUS -> {
                    NotificationManagerCompat.from(context).cancel(EXIT_NOTIFICATION_ID)
                    notify(
                        context = context,
                        id = ENTER_NOTIFICATION_ID,
                        title = "Focus time starting",
                        body = "Tap to switch to Mum Launcher when you're ready.",
                        action = PromptAction.ENTER_FOCUS,
                    )
                }
                PromptAction.EXIT_FOCUS -> {
                    NotificationManagerCompat.from(context).cancel(ENTER_NOTIFICATION_ID)
                    notify(
                        context = context,
                        id = EXIT_NOTIFICATION_ID,
                        title = "Focus time ended",
                        body = "Tap when you're ready to leave focus mode.",
                        action = PromptAction.EXIT_FOCUS,
                    )
                }
            }
        }
    }

    private fun notify(
        context: Context,
        id: Int,
        title: String,
        body: String,
        action: PromptAction,
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            setAction(Intent.ACTION_MAIN)
            addCategory(Intent.CATEGORY_HOME)
            putExtra(EXTRA_PROMPT_ACTION, action.name)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, SCHEDULE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun schedulePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, KioskRecoveryReceiver::class.java).apply {
            action = ACTION_SCHEDULE_CHECK
        }
        return PendingIntent.getBroadcast(
            context,
            4001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelScheduledCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(schedulePendingIntent(context))
    }

    private fun cancelNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancel(ENTER_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(EXIT_NOTIFICATION_ID)
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            SCHEDULE_CHANNEL_ID,
            "Schedule prompts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Prompts to switch into and out of focus launcher mode."
        }
        manager.createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) return false
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notificationsEnabled) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(SCHEDULE_CHANNEL_ID)
            if (channel != null && channel.importance < NotificationManager.IMPORTANCE_HIGH) return false
        }
        return true
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(fallback) }
            }
    }

    fun openExactAlarmSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(fallback) }
            }
    }
}
