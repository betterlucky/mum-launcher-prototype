package com.daveharris.mumlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class LauncherMode {
    SIMPLE,
    SCHEDULED,
}

data class LauncherSettings(
    val pinHash: String? = null,
    val adminPinEnabled: Boolean = false,
    val allowUserContactEditing: Boolean = true,
    val kioskEnabled: Boolean = false,
    val setupComplete: Boolean = false,
    val launcherMode: LauncherMode = LauncherMode.SIMPLE,
    val scheduleDays: Set<Int> = setOf(2, 3, 4, 5, 6),
    val scheduleStartMinutes: Int = 9 * 60,
    val scheduleEndMinutes: Int = 17 * 60,
    val showFocusUntilText: Boolean = false,
    val warnIfScheduleNotificationsOff: Boolean = true,
    val showLauncherAppIcon: Boolean = false,
    val focusSessionActive: Boolean = false,
    val focusSessionAnchor: String? = null,
    val lastSystemEventAtMs: Long = 0L,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val pinHash = stringPreferencesKey("pin_hash")
        val adminPinEnabled = booleanPreferencesKey("admin_pin_enabled")
        val allowUserContactEditing = booleanPreferencesKey("allow_user_contact_editing")
        val kioskEnabled = booleanPreferencesKey("kiosk_enabled")
        val setupComplete = booleanPreferencesKey("setup_complete")
        val launcherMode = stringPreferencesKey("launcher_mode")
        val scheduleDays = stringPreferencesKey("schedule_days")
        val scheduleStartMinutes = intPreferencesKey("schedule_start_minutes")
        val scheduleEndMinutes = intPreferencesKey("schedule_end_minutes")
        val showFocusUntilText = booleanPreferencesKey("show_focus_until_text")
        val warnIfScheduleNotificationsOff = booleanPreferencesKey("warn_if_schedule_notifications_off")
        val showLauncherAppIcon = booleanPreferencesKey("show_launcher_app_icon")
        val focusSessionActive = booleanPreferencesKey("focus_session_active")
        val focusSessionAnchor = stringPreferencesKey("focus_session_anchor")
        val lastSystemEventAtMs = longPreferencesKey("last_system_event_at_ms")
    }

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs: Preferences ->
            LauncherSettings(
                pinHash = prefs[Keys.pinHash],
                adminPinEnabled = prefs[Keys.adminPinEnabled] ?: (prefs[Keys.pinHash] != null),
                allowUserContactEditing = prefs[Keys.allowUserContactEditing] ?: true,
                kioskEnabled = prefs[Keys.kioskEnabled] ?: false,
                setupComplete = prefs[Keys.setupComplete] ?: false,
                launcherMode = prefs[Keys.launcherMode]
                    ?.let { raw -> LauncherMode.entries.firstOrNull { it.name == raw } }
                    ?: LauncherMode.SIMPLE,
                scheduleDays = decodeDays(prefs[Keys.scheduleDays]),
                scheduleStartMinutes = prefs[Keys.scheduleStartMinutes] ?: 9 * 60,
                scheduleEndMinutes = prefs[Keys.scheduleEndMinutes] ?: 17 * 60,
                showFocusUntilText = prefs[Keys.showFocusUntilText] ?: false,
                warnIfScheduleNotificationsOff = prefs[Keys.warnIfScheduleNotificationsOff] ?: true,
                showLauncherAppIcon = prefs[Keys.showLauncherAppIcon] ?: false,
                focusSessionActive = prefs[Keys.focusSessionActive] ?: false,
                focusSessionAnchor = prefs[Keys.focusSessionAnchor],
                lastSystemEventAtMs = prefs[Keys.lastSystemEventAtMs] ?: 0L,
            )
        }

    suspend fun configureAdminPin(pinHash: String?) {
        context.settingsDataStore.edit {
            if (pinHash == null) {
                it[Keys.adminPinEnabled] = false
                it.remove(Keys.pinHash)
            } else {
                it[Keys.adminPinEnabled] = true
                it[Keys.pinHash] = pinHash
            }
        }
    }

    suspend fun setAllowUserContactEditing(allowed: Boolean) {
        context.settingsDataStore.edit { it[Keys.allowUserContactEditing] = allowed }
    }

    suspend fun setKioskEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.kioskEnabled] = enabled }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.setupComplete] = complete }
    }

    suspend fun setLauncherMode(mode: LauncherMode) {
        context.settingsDataStore.edit { it[Keys.launcherMode] = mode.name }
    }

    suspend fun setSchedule(days: Set<Int>, startMinutes: Int, endMinutes: Int) {
        context.settingsDataStore.edit {
            it[Keys.scheduleDays] = encodeDays(days)
            it[Keys.scheduleStartMinutes] = startMinutes
            it[Keys.scheduleEndMinutes] = endMinutes
        }
    }

    suspend fun markSystemEvent(timestampMs: Long) {
        context.settingsDataStore.edit { it[Keys.lastSystemEventAtMs] = timestampMs }
    }

    suspend fun setShowFocusUntilText(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.showFocusUntilText] = enabled }
    }

    suspend fun setWarnIfScheduleNotificationsOff(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.warnIfScheduleNotificationsOff] = enabled }
    }

    suspend fun setShowLauncherAppIcon(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.showLauncherAppIcon] = enabled }
    }

    suspend fun setFocusSession(active: Boolean, anchor: String?) {
        context.settingsDataStore.edit {
            it[Keys.focusSessionActive] = active
            if (anchor == null) {
                it.remove(Keys.focusSessionAnchor)
            } else {
                it[Keys.focusSessionAnchor] = anchor
            }
        }
    }

    companion object {
        private fun encodeDays(days: Set<Int>): String =
            days.sorted().joinToString(",")

        private fun decodeDays(raw: String?): Set<Int> {
            if (raw.isNullOrBlank()) return setOf(2, 3, 4, 5, 6)
            return raw.split(',')
                .mapNotNull { it.toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
                .ifEmpty { setOf(2, 3, 4, 5, 6) }
        }
    }
}
