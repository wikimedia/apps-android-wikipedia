package org.wikipedia.search.interleave

import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class InterleaveSearchAbTest : ABTest("semantic-search-phase-2-interleave", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    private fun isTestActive(): Boolean {
        return Prefs.interleaveSearchIsTestActive || RemoteConfig.config.androidv1?.semanticSearchEnabledv3 ?: false
    }

    private fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    private fun isLanguageSupported(languageCode: String?): Boolean {
        return Prefs.interleaveSearchLanguageOverride || supportedLanguages.any { it.equals(languageCode, true) }
    }

    private val supportedLanguages = RemoteConfig.config.androidv1?.semanticSearchLanguagesv3.orEmpty()

    fun isInterleaveSearchSettingVisible(): Boolean {
        return isTestActive() && isTestGroupUser()
    }

    fun isInterleaveSearchEligible(languageCode: String?): Boolean {
        return isTestActive() && isLanguageSupported(languageCode)
    }

    fun isInterleaveSearchEnabled(languageCode: String?): Boolean {
        return isInterleaveSearchEligible(languageCode) && isTestGroupUser() && Prefs.isInterleaveSearchEnabled
    }

    fun runIfEligible(function: () -> Unit) {
        if (WikipediaApp.instance.languageState.appLanguageCodes.any { isInterleaveSearchEligible(it) }) {
            function.invoke()
        }
    }

    fun setExperimentIfEligible(instrument: InstrumentImpl) {
        runIfEligible {
            instrument.setExperiment(TestKitchenAdapter.getExperiment(this))
        }
    }
}
