package org.wikipedia.search

import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class HybridSearchAbCTest : ABTest("hybridSearch", GROUP_SIZE_3) {

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> GROUP_LEXICAL_SEMANTIC
            GROUP_3 -> GROUP_SEMANTIC_LEXICAL
            else -> GROUP_CONTROL
        }
    }

    fun isTestActive(): Boolean {
        return RemoteConfig.config.androidv1?.hybridSearchEnabled ?: true
    }

    fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return (RemoteConfig.config.androidv1?.hybridSearchLanguages ?: supportedLanguages).any { it.equals(languageCode, true) }
    }

    fun shouldShowOnboarding(languageCode: String?): Boolean {
        return isTestGroupUser() && isLanguageSupported(languageCode) && !Prefs.isHybridSearchOnboardingShown
    }

    private val supportedLanguages = listOf(
        "el"
    )

    fun isHybridSearchEnabled(languageCode: String?): Boolean {
        return isTestActive() && Prefs.isHybridSearchEnabled && isTestGroupUser() && isLanguageSupported(languageCode)
    }

    companion object {
        const val GROUP_CONTROL = "control"
        const val GROUP_LEXICAL_SEMANTIC = "lexicalSemantic"
        const val GROUP_SEMANTIC_LEXICAL = "semanticLexical"
    }
}
