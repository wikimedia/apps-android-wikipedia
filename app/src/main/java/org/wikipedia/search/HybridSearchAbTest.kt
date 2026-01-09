package org.wikipedia.search

import org.wikipedia.analytics.ABTest

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

    // TODO: check with PM about countries vs language codes
    val availableLanguages = listOf(
        "en", "fr", "pt"
    )
}
