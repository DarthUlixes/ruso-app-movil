package com.example.rusoit.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rusoit_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN] = token
        }
    }

    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[JWT_TOKEN]
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
