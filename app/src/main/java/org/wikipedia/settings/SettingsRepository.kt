package org.wikipedia.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wikipedia.WikipediaApp
import org.wikipedia.extensions.dataStore

object SettingRepository {
    private val preferenceKey = stringSetPreferencesKey("home_hidden_modules")
    private val dataStore = WikipediaApp.instance.dataStore
    val hiddenModules: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[preferenceKey] ?: emptySet()
        }

    suspend fun addHiddenModule(moduleKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[preferenceKey] ?: emptySet()
            preferences[preferenceKey] = current + moduleKey
        }
    }

    suspend fun removeHiddenModule(moduleKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[preferenceKey] ?: emptySet()
            preferences[preferenceKey] = current - moduleKey
        }
    }
}
