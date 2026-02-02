package org.wikipedia.search

import org.wikipedia.analytics.ABTest
import org.wikipedia.settings.Prefs

class HybridSearchAbTest : ABTest("hybridSearch", GROUP_SIZE_3) {

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> GROUP_LEXICAL_SEMANTIC // test group B
            GROUP_3 -> GROUP_SEMANTIC_LEXICAL // test group C
            else -> GROUP_CONTROL
        }
    }

    fun isTestGroupUser(): Boolean {
        return group != GROUP_1
    }

    val supportedLanguages = listOf(
        "en", "fr", "pt"
    )

    fun isHybridSearchEnabled(languageCode: String?): Boolean {
        return Prefs.isHybridSearchEnabled && isTestGroupUser() && supportedLanguages.any { it.equals(languageCode, true) }
    }

    companion object {
        const val GROUP_CONTROL = "control"
        const val GROUP_LEXICAL_SEMANTIC = "lexicalSemantic"
        const val GROUP_SEMANTIC_LEXICAL = "semanticLexical"
    }
}
