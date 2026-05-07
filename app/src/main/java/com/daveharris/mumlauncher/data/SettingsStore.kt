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

enum class LauncherMode { SIMPLE, RELAXED }

data class LauncherSettings(
    val pinHash: String? = null,
    val allowUserContactEditing: Boolean = true,
    val setupComplete: Boolean = false,
    val nativeLauncherPackage: String? = null,
    val nativeLauncherLabel: String? = null,
    val showRelaxedButton: Boolean = true,
    val relaxedScrollHorizontal: Boolean = false,
    val schedulingEnabled: Boolean = false,
    val scheduleDays: Set<Int> = setOf(2, 3, 4, 5, 6),
    val scheduleStartMinutes: Int = 9 * 60,
    val scheduleEndMinutes: Int = 17 * 60,
    val scheduledMode: LauncherMode = LauncherMode.SIMPLE,
    val focusSessionActive: Boolean = false,
    val focusSessionAnchor: String? = null,
    val scheduleAudioAlert: Boolean = false,
    val allowUserSkipExtend: Boolean = false,
    val scheduleSkippedUntilMs: Long = 0L,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val pinHash = stringPreferencesKey("pin_hash")
        val allowUserContactEditing = booleanPreferencesKey("allow_user_contact_editing")
        val setupComplete = booleanPreferencesKey("setup_complete")
        val nativeLauncherPackage = stringPreferencesKey("native_launcher_package")
        val nativeLauncherLabel = stringPreferencesKey("native_launcher_label")
        val showRelaxedButton = booleanPreferencesKey("show_relaxed_button")
        val relaxedScrollHorizontal = booleanPreferencesKey("relaxed_scroll_horizontal")
        val schedulingEnabled = booleanPreferencesKey("scheduling_enabled")
        val scheduleDays = stringPreferencesKey("schedule_days")
        val scheduleStartMinutes = intPreferencesKey("schedule_start_minutes")
        val scheduleEndMinutes = intPreferencesKey("schedule_end_minutes")
        val scheduledMode = stringPreferencesKey("scheduled_mode")
        val focusSessionActive = booleanPreferencesKey("focus_session_active")
        val focusSessionAnchor = stringPreferencesKey("focus_session_anchor")
        val scheduleAudioAlert = booleanPreferencesKey("schedule_audio_alert")
        val allowUserSkipExtend = booleanPreferencesKey("allow_user_skip_extend")
        val scheduleSkippedUntilMs = longPreferencesKey("schedule_skipped_until_ms")
    }

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs: Preferences ->
            LauncherSettings(
                pinHash = prefs[Keys.pinHash],
                allowUserContactEditing = prefs[Keys.allowUserContactEditing] ?: true,
                setupComplete = prefs[Keys.setupComplete] ?: false,
                nativeLauncherPackage = prefs[Keys.nativeLauncherPackage],
                nativeLauncherLabel = prefs[Keys.nativeLauncherLabel],
                showRelaxedButton = prefs[Keys.showRelaxedButton] ?: true,
                relaxedScrollHorizontal = prefs[Keys.relaxedScrollHorizontal] ?: false,
                schedulingEnabled = prefs[Keys.schedulingEnabled] ?: false,
                scheduleDays = decodeDays(prefs[Keys.scheduleDays]),
                scheduleStartMinutes = prefs[Keys.scheduleStartMinutes] ?: (9 * 60),
                scheduleEndMinutes = prefs[Keys.scheduleEndMinutes] ?: (17 * 60),
                scheduledMode = prefs[Keys.scheduledMode]
                    ?.let { raw -> LauncherMode.entries.firstOrNull { it.name == raw } }
                    ?: LauncherMode.SIMPLE,
                focusSessionActive = prefs[Keys.focusSessionActive] ?: false,
                focusSessionAnchor = prefs[Keys.focusSessionAnchor],
                scheduleAudioAlert = prefs[Keys.scheduleAudioAlert] ?: false,
                allowUserSkipExtend = prefs[Keys.allowUserSkipExtend] ?: false,
                scheduleSkippedUntilMs = prefs[Keys.scheduleSkippedUntilMs] ?: 0L,
            )
        }

    suspend fun setPinHash(hash: String) {
        context.settingsDataStore.edit { it[Keys.pinHash] = hash }
    }

    suspend fun setAllowUserContactEditing(allowed: Boolean) {
        context.settingsDataStore.edit { it[Keys.allowUserContactEditing] = allowed }
    }

    suspend fun setSetupComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.setupComplete] = complete }
    }

    suspend fun setNativeLauncher(packageName: String, label: String) {
        context.settingsDataStore.edit {
            it[Keys.nativeLauncherPackage] = packageName
            it[Keys.nativeLauncherLabel] = label
        }
    }

    suspend fun setShowRelaxedButton(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.showRelaxedButton] = show }
    }

    suspend fun setRelaxedScrollHorizontal(horizontal: Boolean) {
        context.settingsDataStore.edit { it[Keys.relaxedScrollHorizontal] = horizontal }
    }

    suspend fun setSchedulingEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.schedulingEnabled] = enabled }
    }

    suspend fun setScheduleDays(days: Set<Int>) {
        context.settingsDataStore.edit { it[Keys.scheduleDays] = encodeDays(days) }
    }

    suspend fun setScheduleStartMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.scheduleStartMinutes] = minutes }
    }

    suspend fun setScheduleEndMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.scheduleEndMinutes] = minutes }
    }

    suspend fun setScheduledMode(mode: LauncherMode) {
        context.settingsDataStore.edit { it[Keys.scheduledMode] = mode.name }
    }

    suspend fun setFocusSession(active: Boolean, anchor: String?) {
        context.settingsDataStore.edit {
            it[Keys.focusSessionActive] = active
            if (anchor != null) it[Keys.focusSessionAnchor] = anchor
            else it.remove(Keys.focusSessionAnchor)
        }
    }

    suspend fun setScheduleAudioAlert(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.scheduleAudioAlert] = enabled }
    }

    suspend fun setAllowUserSkipExtend(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.allowUserSkipExtend] = enabled }
    }

    suspend fun setScheduleSkippedUntil(ms: Long) {
        context.settingsDataStore.edit { it[Keys.scheduleSkippedUntilMs] = ms }
    }
}

private fun decodeDays(raw: String?): Set<Int> {
    if (raw.isNullOrBlank()) return setOf(2, 3, 4, 5, 6)
    return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
}

private fun encodeDays(days: Set<Int>): String = days.sorted().joinToString(",")
