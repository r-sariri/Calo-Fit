package com.example.calofit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calofit_user_preferences")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("app_language") // "fa" or "en"
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "LIGHT", "DARK", "SYSTEM"
        val KEY_FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "fa"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "LIGHT"
    }

    val firstLaunchCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_LAUNCH_COMPLETED] ?: false
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = lang
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setFirstLaunchCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIRST_LAUNCH_COMPLETED] = completed
        }
    }
}
