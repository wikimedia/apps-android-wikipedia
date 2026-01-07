package org.wikipedia.search

import org.wikipedia.analytics.ABTest

class SemanticSearchAbTest : ABTest("semanticSearch", GROUP_SIZE_2) {

    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    val enabledCountries = listOf(
        "EN", "FR", "PT"
    )
}
