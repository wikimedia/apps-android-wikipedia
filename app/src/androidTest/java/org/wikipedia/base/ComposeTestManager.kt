package org.wikipedia.base

import androidx.compose.ui.test.junit4.ComposeTestRule

object ComposeTestManager {
    private var _composeTestRule: ComposeTestRule? = null

    fun setComposeTestRule(rule: ComposeTestRule) {
        _composeTestRule = rule
    }

    fun getComposeTestRule(): ComposeTestRule {
        return _composeTestRule ?: throw IllegalStateException("ComposeTestRule not initialized")
    }
}
