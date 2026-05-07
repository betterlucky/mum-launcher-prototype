package com.daveharris.mumlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.presetsDataStore by preferencesDataStore(name = "presets")

class PresetRepository(private val context: Context) {
    private val mutex = Mutex()
    private val presetsKey = stringPreferencesKey("presets_json")

    fun observePresets(): Flow<List<Preset>> = context.presetsDataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs ->
            decode(prefs[presetsKey].orEmpty())
        }

    suspend fun add(name: String, apps: List<String> = emptyList()): Long = mutex.withLock {
        val presets = observePresets().first()
        val nextId = (presets.maxOfOrNull { it.id } ?: 0L) + 1L
        save(presets + Preset(id = nextId, name = name.trim(), apps = apps))
        nextId
    }

    suspend fun update(preset: Preset) = mutex.withLock {
        val presets = observePresets().first()
        save(presets.map { if (it.id == preset.id) preset.copy(name = preset.name.trim()) else it })
    }

    suspend fun delete(id: Long) = mutex.withLock {
        val presets = observePresets().first()
        save(presets.filterNot { it.id == id })
    }

    private suspend fun save(presets: List<Preset>) {
        context.presetsDataStore.edit { it[presetsKey] = encode(presets) }
    }

    private fun encode(presets: List<Preset>): String {
        val json = JSONArray()
        presets.forEach { preset ->
            json.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name)
                    put("apps", JSONArray().also { arr -> preset.apps.forEach { arr.put(it) } })
                },
            )
        }
        return json.toString()
    }

    private fun decode(raw: String): List<Preset> {
        if (raw.isBlank()) return emptyList()
        val json = JSONArray(raw)
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            val appsArray = item.optJSONArray("apps") ?: JSONArray()
            Preset(
                id = item.optLong("id"),
                name = item.optString("name"),
                apps = List(appsArray.length()) { i -> appsArray.getString(i) },
            )
        }
    }
}
