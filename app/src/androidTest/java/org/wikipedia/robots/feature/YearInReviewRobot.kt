package org.wikipedia.robots.feature

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wikipedia.yearinreview.YearInReviewScreenContent
import org.wikipedia.yearinreview.readCountData

class YearInReviewRobot {

    private lateinit var composeTestRule: ComposeContentTestRule

    fun setComposeTestRule(rule: ComposeContentTestRule) = apply {
        this.composeTestRule = rule
        return@apply
    }

    fun assertHeadlineIsRtL() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl)
            ) {
            YearInReviewScreenContent(
                innerPadding = PaddingValues(48.dp),
                screenData = readCountData)
            }
        }

        composeTestRule.onNodeWithTag("hello")
        }
    }

/*
fun assertHeadlineIsLTR() {
        var currentTextDirection: LayoutDirection? = null
        composeTestRule.setContent {
            YearInReviewScreenContent(
                innerPadding = PaddingValues(48.dp),
                screenData = readCountData
            ).run{ currentTextDirection = LocalLayoutDirection.current}
        }
        composeTestRule.onNodeWithTag("hello").also {
            assert(LocalLayoutDirection.current == LayoutDirection.Ltr)
        }
        /*
        composeTestRule.runOnIdle {
            currentTextDirection = LocalLayoutDirection.current
        }
        assert(currentTextDirection == LayoutDirection.Ltr) */
    }
 */
