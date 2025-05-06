package org.wikipedia.robots.feature

import android.util.Log
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.wikipedia.base.TestConfig
import org.wikipedia.base.base.BaseRobot

class YearInReviewRobot() : BaseRobot() {
    private var leftMarginLTR: Float = 0f
    private var rightMarginLTR: Float = 0f
    private var headlineRTL: Float = 0f

    fun getStarted() = apply {
        composeTestRule.onNodeWithText("Get Started").performClick()
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun setLeftMargin() = apply {
        val node = composeTestRule.onNodeWithTag("headline_text").fetchSemanticsNode()
        leftMarginLTR = node
            .layoutInfo
            .coordinates
            .positionInRoot()
            .x

        Log.d("x-pos-headline", leftMarginLTR.toString())
    }

    fun setRightMargin() = apply {
        val node = composeTestRule.onNodeWithTag("information_icon").fetchSemanticsNode()
        rightMarginLTR = node
            .layoutInfo
            .coordinates
            .positionInRoot()
            .x

        Log.d("x-pos-icon", rightMarginLTR.toString())
    }

    fun checkLanguageRTL() = apply {

        val node = composeTestRule.onNodeWithTag("headline_text").fetchSemanticsNode()
        headlineRTL = node
            .layoutInfo
            .coordinates
            .positionInRoot()
            .x

        Log.d("x-pos-headlineRTL", headlineRTL.toString())
    }
}
