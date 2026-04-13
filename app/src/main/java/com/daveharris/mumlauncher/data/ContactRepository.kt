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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.contactsDataStore by preferencesDataStore(name = "contacts")

class ContactRepository(private val context: Context) {
    private val contactsKey = stringPreferencesKey("contacts_json")

    fun observeContacts(): Flow<List<Contact>> = context.contactsDataStore.data
        .catch {
            if (it is IOException) emit(emptyPreferences()) else throw it
        }
        .map { prefs ->
            decodeContacts(prefs[contactsKey].orEmpty())
        }

    suspend fun ensureSeedData() {
        val existing = observeContacts().first()
        if (existing.isEmpty()) {
            saveContacts(
                listOf(
                    Contact(
                        id = 1,
                        displayName = "Add Mum's first contact",
                        phoneNumber = "01234 567890",
                    ),
                ),
            )
        }
    }

    suspend fun add(displayName: String, phoneNumber: String) {
        val contacts = observeContacts().first()
        val nextId = (contacts.maxOfOrNull { it.id } ?: 0L) + 1L
        saveContacts(
            contacts + Contact(
                id = nextId,
                displayName = displayName.trim(),
                phoneNumber = phoneNumber.trim(),
                sortOrder = contacts.size,
            ),
        )
    }

    suspend fun update(contact: Contact) {
        val contacts = observeContacts().first()
        saveContacts(
            contacts.map {
                if (it.id == contact.id) {
                    contact.copy(
                        displayName = contact.displayName.trim(),
                        phoneNumber = contact.phoneNumber.trim(),
                    )
                } else {
                    it
                }
            },
        )
    }

    suspend fun delete(contact: Contact) {
        val contacts = observeContacts().first()
        saveContacts(contacts.filterNot { it.id == contact.id })
    }

    private suspend fun saveContacts(contacts: List<Contact>) {
        context.contactsDataStore.edit { prefs ->
            prefs[contactsKey] = encodeContacts(contacts)
        }
    }

    private fun encodeContacts(contacts: List<Contact>): String {
        val json = JSONArray()
        contacts.forEach { contact ->
            json.put(
                JSONObject().apply {
                    put("id", contact.id)
                    put("displayName", contact.displayName)
                    put("phoneNumber", contact.phoneNumber)
                    put("callable", contact.callable)
                    put("messageable", contact.messageable)
                    put("sortOrder", contact.sortOrder)
                },
            )
        }
        return json.toString()
    }

    private fun decodeContacts(raw: String): List<Contact> {
        if (raw.isBlank()) return emptyList()
        val json = JSONArray(raw)
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            Contact(
                id = item.optLong("id"),
                displayName = item.optString("displayName"),
                phoneNumber = item.optString("phoneNumber"),
                callable = item.optBoolean("callable", true),
                messageable = item.optBoolean("messageable", true),
                sortOrder = item.optInt("sortOrder", index),
            )
        }.sortedWith(compareBy<Contact> { it.sortOrder }.thenBy { it.displayName.lowercase() })
    }
}
