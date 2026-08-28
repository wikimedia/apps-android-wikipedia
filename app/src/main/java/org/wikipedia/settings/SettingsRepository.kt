package org.wikipedia.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wikipedia.WikipediaApp
import org.wikipedia.extensions.dataStore

object SettingsRepository {
    private const val MAX_HIDDEN_CARDS = 100

    private val hiddenModulesKey = stringSetPreferencesKey("home_hidden_modules")
    private val hiddenCardsKey = stringSetPreferencesKey("home_hidden_cards")
    private val dataStore = WikipediaApp.instance.dataStore

    val hiddenModules: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[hiddenModulesKey] ?: emptySet()
        }

    val hiddenCards: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[hiddenCardsKey] ?: emptySet()
        }

    suspend fun addHiddenModule(moduleKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[hiddenModulesKey] ?: emptySet()
            preferences[hiddenModulesKey] = current + moduleKey
        }
    }

    suspend fun removeHiddenModule(moduleKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[hiddenModulesKey] ?: emptySet()
            preferences[hiddenModulesKey] = current - moduleKey
        }
    }

    suspend fun addHiddenCard(cardKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[hiddenCardsKey] ?: emptySet()
            val trimmed = if (current.size >= MAX_HIDDEN_CARDS) {
                current.drop(1).toSet()
            } else {
                current
            }
            preferences[hiddenCardsKey] = trimmed + cardKey
        }
    }

    suspend fun removeHiddenCard(cardKey: String) {
        dataStore.edit { preferences ->
            val current = preferences[hiddenCardsKey] ?: emptySet()
            preferences[hiddenCardsKey] = current - cardKey
        }
    }

    /**
     * One-time migration of hidden card keys from the legacy [Prefs.hiddenCards]
     * clears the legacy list after copying, so subsequent calls are ignored.
     */
    suspend fun migrateLegacyHiddenCards() {
        val legacy = Prefs.hiddenCards
        if (legacy.isEmpty()) {
            return
        }
        dataStore.edit { preferences ->
            val current = preferences[hiddenCardsKey] ?: emptySet()
            preferences[hiddenCardsKey] = (current + legacy).toList().takeLast(MAX_HIDDEN_CARDS).toSet()
        }
        Prefs.hiddenCards = emptyList()
    }
}
