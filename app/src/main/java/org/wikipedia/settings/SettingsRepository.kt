package org.wikipedia.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wikipedia.WikipediaApp

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wikipedia_settings")

object SettingRepository {
    private val HIDDEN_MODULES_KEY = stringSetPreferencesKey("hidden_modules")
    val hiddenModules: Flow<Set<String>> = WikipediaApp.instance.dataStore.data
        .map { preferences ->
            preferences[HIDDEN_MODULES_KEY] ?: emptySet()
        }

    suspend fun addHiddenModule(moduleKey: String) {
        WikipediaApp.instance.dataStore.edit { preferences ->
            val current = preferences[HIDDEN_MODULES_KEY] ?: emptySet()
            preferences[HIDDEN_MODULES_KEY] = current + moduleKey
        }
    }

    suspend fun removeHiddenModule(moduleKey: String) {
        WikipediaApp.instance.dataStore.edit { preferences ->
            val current = preferences[HIDDEN_MODULES_KEY] ?: emptySet()
            preferences[HIDDEN_MODULES_KEY] = current - moduleKey
        }
    }
}
