package org.wikipedia.search

import org.wikipedia.analytics.ABTest

class SemanticSearchAbTest : ABTest("semanticSearch", GROUP_SIZE_2) {
    private val supportedLanguages = listOf("en", "fr", "pt")
    val enabledCountries = listOf(
        "EN", "FR", "PT"
    )

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    fun isSemanticSearchEnabled(languageCode: String?): Boolean {
        return isTestGroupUser() && supportedLanguages.any { it.equals(languageCode, true) }
    }
}
