package org.wikipedia.search.semantic

import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class SemanticSearchAbTest : ABTest("apps_semantic_search", GROUP_SIZE_2) {
    // TODO: confirm with data about the abTestName & group name
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b"
            else -> "a"
        }
    }

    fun isTestActive(): Boolean {
        return RemoteConfig.config.androidv1?.hybridSearchEnabled ?: false // TODO: update to the new variable.
    }

    fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = listOf(
        "ja", "ab", "fr"
    )

    fun isSemanticSearchEnabled(languageCode: String?): Boolean {
        return isTestActive() && Prefs.isSemanticSearchEnabled && isTestGroupUser() && isLanguageSupported(languageCode)
    }
}
