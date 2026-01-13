package org.wikipedia.search

import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.Prefs

class HybridSearchAbTest : ABTest("hybridSearch", GROUP_SIZE_2) {

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    val supportedLanguages = listOf(
        "en", "fr", "pt"
    )

    fun isHybridSearchEnabled(languageCode: String?): Boolean {
        return Prefs.isHybridSearchEnabled && isTestGroupUser() && supportedLanguages.any { it.equals(languageCode, true) }
    }
}
