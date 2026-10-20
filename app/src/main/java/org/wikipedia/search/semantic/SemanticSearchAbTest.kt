package org.wikipedia.search.semantic

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class SemanticSearchAbTest : ABTest("semantic-search-phase-2", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    private fun isTestActive(): Boolean {
        return Prefs.semanticSearchIsTestActive || RemoteConfig.config.androidv1?.semanticSearchEnabledv2 ?: false
    }

    private fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return Prefs.semanticSearchLanguageOverride || supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = RemoteConfig.config.androidv1?.semanticSearchLanguagesv2.orEmpty()

    fun isSemanticSearchSettingVisible(): Boolean {
        return isTestActive() && isTestGroupUser()
    }

    fun isSemanticSearchEligible(languageCode: String?): Boolean {
        return isTestActive() && isLanguageSupported(languageCode)
    }

    fun isSemanticSearchEnabled(languageCode: String?): Boolean {
        return isSemanticSearchEligible(languageCode) && isTestGroupUser() && Prefs.isSemanticSearchEnabled
    }

    fun runIfEligible(function: () -> Unit) {
        if (WikipediaApp.instance.languageState.appLanguageCodes.any { isSemanticSearchEligible(it) }) {
            function.invoke()
        }
    }

    fun setExperimentIfEligible(instrument: InstrumentImpl) {
        runIfEligible {
            instrument.setExperiment(TestKitchenAdapter.getExperiment(this))
        }
    }
}
