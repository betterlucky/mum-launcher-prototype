package com.mumslauncher.app

import com.mumslauncher.app.data.LauncherSettings
import java.util.Calendar
import java.util.Locale

enum class PromptAction {
    ENTER_FOCUS,
    EXIT_FOCUS;

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

fun currentScheduledWindow(settings: LauncherSettings, nowMs: Long): ScheduledWindow? {
    if (!settings.schedulingEnabled || settings.scheduleDays.isEmpty()) return null
    return scheduledWindowsAround(settings, nowMs)
        .filter { nowMs in it.startMs until it.endMs }
        .minByOrNull { it.endMs }
}

fun mostRecentEndedWindow(settings: LauncherSettings, nowMs: Long): ScheduledWindow? {
    if (!settings.schedulingEnabled || settings.scheduleDays.isEmpty()) return null
    return scheduledWindowsAround(settings, nowMs)
        .filter { it.endMs <= nowMs }
        .maxByOrNull { it.endMs }
}

fun currentDuePrompt(settings: LauncherSettings, nowMs: Long): DuePrompt? {
    if (!settings.schedulingEnabled) return null
    if (nowMs < settings.scheduleSkippedUntilMs) return null

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

fun nextScheduleBoundaryMillis(settings: LauncherSettings, nowMs: Long): Long? {
    if (!settings.schedulingEnabled || settings.scheduleDays.isEmpty()) return null

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

private fun scheduledWindowsAround(settings: LauncherSettings, nowMs: Long): List<ScheduledWindow> {
    val windows = mutableListOf<ScheduledWindow>()
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

private fun anchorForStart(calendar: Calendar): String = String.format(
    Locale.US,
    "%04d-%02d-%02d-%02d-%02d",
    calendar.get(Calendar.YEAR),
    calendar.get(Calendar.MONTH) + 1,
    calendar.get(Calendar.DAY_OF_MONTH),
    calendar.get(Calendar.HOUR_OF_DAY),
    calendar.get(Calendar.MINUTE),
)

private fun boundaryForDay(nowMs: Long, dayOffset: Int, minutesOfDay: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = nowMs
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        set(Calendar.MINUTE, minutesOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
