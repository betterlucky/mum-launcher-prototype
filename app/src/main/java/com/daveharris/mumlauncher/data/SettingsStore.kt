package com.daveharris.mumlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class LauncherSettings(
    val pinHash: String? = null,
    val allowUserContactEditing: Boolean = true,
    val kioskEnabled: Boolean = false,
    val setupComplete: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val pinHash = stringPreferencesKey("pin_hash")
        val allowUserContactEditing = booleanPreferencesKey("allow_user_contact_editing")
        val kioskEnabled = booleanPreferencesKey("kiosk_enabled")
        val setupComplete = booleanPreferencesKey("setup_complete")
    }

    val settings: Flow<LauncherSettings> = context.settingsDataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs: Preferences ->
            LauncherSettings(
                pinHash = prefs[Keys.pinHash],
                allowUserContactEditing = prefs[Keys.allowUserContactEditing] ?: true,
                kioskEnabled = prefs[Keys.kioskEnabled] ?: false,
                setupComplete = prefs[Keys.setupComplete] ?: false,
            )
        }

    suspend fun setPinHash(hash: String) {
        context.settingsDataStore.edit { it[Keys.pinHash] = hash }
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
}
