package org.wikipedia.analytics

import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.util.log.L

class AppLanguageSettingsFunnel : TimedFunnel(WikipediaApp.getInstance(), SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    fun logLanguageSetting(source: InvokeSource, initialLanguageList: String, finalLanguageList: String,
                           interactionsCount: Int, searched: Boolean) {
        log(
                "source", source.value,
                "initial", initialLanguageList,
                "final", finalLanguageList,
                "interactions", interactionsCount,
                "searched", searched
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppLanguageSettings"
        private const val REV_ID = 18113720
    }
}
