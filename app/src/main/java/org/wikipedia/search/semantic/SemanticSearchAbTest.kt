package org.wikipedia.search.semantic

import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class SemanticSearchAbTest : ABTest("semantic-search-phase-2", GROUP_SIZE_2) {
    // TODO: confirm with data about the abTestName & group name
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
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

    fun isSemanticSearchEligible(languageCode: String?): Boolean {
        return isTestActive() && isLanguageSupported(languageCode)
    }

    fun isSemanticSearchEnabled(languageCode: String?): Boolean {
        return isSemanticSearchEligible(languageCode) && isTestGroupUser() && Prefs.isSemanticSearchEnabled
    }

    fun shouldSendEvent(function: () -> (Unit)) {
        if (WikipediaApp.instance.languageState.appLanguageCodes.any { isSemanticSearchEligible(it) }) {
            function.invoke()
        }
    }

    companion object {
        private const val DEFAULT_QUOTATION_MARK = "«"
        private val quotationMarkMap = mapOf(
            "ja" to "『",
            "ar" to "❝",
            "fr" to "«"
        )

        fun getQuotationMark(languageCode: String): String {
            return quotationMarkMap[languageCode] ?: DEFAULT_QUOTATION_MARK
        }
    }
}
