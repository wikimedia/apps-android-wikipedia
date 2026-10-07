package org.wikipedia.search.semantic

import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class SemanticSearchAbTest : ABTest("semantic-search-phase-2", GROUP_SIZE_2) {
    // TODO: confirm with data about the abTestName & group name
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b"
            else -> "a"
        }
    }

    private fun isTestActive(): Boolean {
        // TODO: remove the Prefs check before release
        if (Prefs.semanticSearchIsTestActive) {
            return true
        }
        return RemoteConfig.config.androidv1?.hybridSearchEnabled ?: false // TODO: update to the new variable.
    }

    private fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        // TODO: remove the Prefs check before release
        if (Prefs.semanticSearchLanguageOverride) {
            return true
        }
        return supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = listOf(
        "ja", "ar", "fr"
    )

    fun isSemanticSearchSettingVisible(): Boolean {
        return isTestActive() && isTestGroupUser()
    }

    fun isSemanticSearchEnabled(languageCode: String?): Boolean {
        return isTestActive() && Prefs.isSemanticSearchEnabled && isTestGroupUser() && isLanguageSupported(languageCode)
    }
}
