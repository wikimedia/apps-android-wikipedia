package org.wikipedia.robots.feature

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.theme.Theme

class TabsRobot : BaseRobot() {
    fun removeTab(position: Int) = apply {
        clickOnSpecificItemInList(
            listId = R.id.tabRecyclerView,
            itemId = R.id.tabCloseButton,
            position = position
        )
    }

    fun launchTabsScreen() = apply {
        clickOnDisplayedView(R.id.page_toolbar_button_tabs)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun createNewTabWithContentDescription(text: String) = apply {
        clickOnDisplayedViewWithContentDescription(text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyTabCount(count: Int) = apply {
        checkWithTextIsDisplayed(R.id.tabsCountText, count.toString())
    }

    fun assertColorOfTabsTitle(position: Int, theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.PRIMARY)
        assertColorForChildItemInAList(
            listId = R.id.tabRecyclerView,
            childItemId = R.id.tabArticleTitle,
            position = position,
            colorResId = color
        )
    }
}
