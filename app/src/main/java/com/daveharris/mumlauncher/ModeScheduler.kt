package com.daveharris.mumlauncher

import android.content.Context
import android.content.Intent
import com.daveharris.mumlauncher.data.LauncherMode
import com.daveharris.mumlauncher.data.LauncherSettings
import java.util.Calendar
import java.util.Locale

enum class PromptAction {
    ENTER_FOCUS,
    EXIT_FOCUS,
    ;

    companion object {
        fun from(raw: String?): PromptAction? = entries.firstOrNull { it.name == raw }
    }
}

data class ScheduledWindow(
    val startMs: Long,
    val endMs: Long,
    val anchor: String,
)

data class DuePrompt(
    val action: PromptAction,
    val anchor: String,
)

fun shouldUseFocusLauncherNow(
    settings: LauncherSettings,
    currentTimeMs: Long,
): Boolean {
    if (settings.launcherMode != LauncherMode.SCHEDULED) return settings.launcherMode == LauncherMode.SIMPLE

    val calendar = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    val today = calendar.get(Calendar.DAY_OF_WEEK)
    val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val start = settings.scheduleStartMinutes
    val end = settings.scheduleEndMinutes

    if (start == end) return settings.scheduleDays.contains(today)

    return if (start < end) {
        settings.scheduleDays.contains(today) && nowMinutes in start until end
    } else {
        val previousDay = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1
        (settings.scheduleDays.contains(today) && nowMinutes >= start) ||
            (settings.scheduleDays.contains(previousDay) && nowMinutes < end)
    }
}

fun currentScheduledWindow(
    settings: LauncherSettings,
    nowMs: Long,
): ScheduledWindow? {
    if (settings.launcherMode != LauncherMode.SCHEDULED || settings.scheduleDays.isEmpty()) return null
    return scheduledWindowsAround(settings, nowMs)
        .filter { nowMs in it.startMs until it.endMs }
        .minByOrNull { it.endMs }
}

fun mostRecentEndedWindow(
    settings: LauncherSettings,
    nowMs: Long,
): ScheduledWindow? {
    if (settings.launcherMode != LauncherMode.SCHEDULED || settings.scheduleDays.isEmpty()) return null
    return scheduledWindowsAround(settings, nowMs)
        .filter { it.endMs <= nowMs }
        .maxByOrNull { it.endMs }
}

fun currentDuePrompt(
    settings: LauncherSettings,
    nowMs: Long,
): DuePrompt? {
    val activeWindow = currentScheduledWindow(settings, nowMs)
    if (activeWindow != null &&
        (!settings.focusSessionActive || settings.focusSessionAnchor != activeWindow.anchor)
    ) {
        return DuePrompt(PromptAction.ENTER_FOCUS, activeWindow.anchor)
    }

    val recentWindow = mostRecentEndedWindow(settings, nowMs)
    if (recentWindow != null &&
        settings.focusSessionActive &&
        settings.focusSessionAnchor == recentWindow.anchor
    ) {
        return DuePrompt(PromptAction.EXIT_FOCUS, recentWindow.anchor)
    }

    return null
}

fun nextScheduleBoundaryMillis(
    settings: LauncherSettings,
    nowMs: Long,
): Long? {
    if (settings.launcherMode != LauncherMode.SCHEDULED || settings.scheduleDays.isEmpty()) return null

    val candidates = mutableListOf<Long>()
    val now = Calendar.getInstance().apply { timeInMillis = nowMs }
    repeat(8) { dayOffset ->
        val day = (now.get(Calendar.DAY_OF_WEEK) + dayOffset - 1) % 7 + 1
        if (settings.scheduleDays.contains(day)) {
            candidates += boundaryForDay(nowMs, dayOffset, settings.scheduleStartMinutes)
            candidates += boundaryForDay(nowMs, dayOffset, settings.scheduleEndMinutes)
        }
    }

    return candidates.filter { it > nowMs + 30_000L }.minOrNull()
}

private fun scheduledWindowsAround(
    settings: LauncherSettings,
    nowMs: Long,
): List<ScheduledWindow> {
    val windows = mutableListOf<ScheduledWindow>()
    val base = Calendar.getInstance().apply { timeInMillis = nowMs }
    repeat(15) { index ->
        val dayOffset = index - 7
        val scheduledDay = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        if (!settings.scheduleDays.contains(scheduledDay.get(Calendar.DAY_OF_WEEK))) return@repeat

        val start = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, settings.scheduleStartMinutes / 60)
            set(Calendar.MINUTE, settings.scheduleStartMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            set(Calendar.HOUR_OF_DAY, settings.scheduleEndMinutes / 60)
            set(Calendar.MINUTE, settings.scheduleEndMinutes % 60)
            if (settings.scheduleStartMinutes >= settings.scheduleEndMinutes) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        windows += ScheduledWindow(
            startMs = start.timeInMillis,
            endMs = end.timeInMillis,
            anchor = anchorForStart(start),
        )
    }
    return windows
}

private fun anchorForStart(calendar: Calendar): String {
    return String.format(
        Locale.US,
        "%04d-%02d-%02d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
    )
}

private fun boundaryForDay(nowMs: Long, dayOffset: Int, minutesOfDay: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = nowMs
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        set(Calendar.MINUTE, minutesOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun isAppDefaultLauncher(context: Context): Boolean {
    val resolveInfo = context.packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        0,
    )
    return resolveInfo?.activityInfo?.packageName == context.packageName
}
