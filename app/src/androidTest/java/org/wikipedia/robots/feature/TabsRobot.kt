package org.wikipedia.robots.feature

import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

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

    fun assertColorOfTabsTitle(position: Int) = apply {
        assertColorForChildItemInAList(
            listId = R.id.tabRecyclerView,
            childItemId = R.id.tabArticleTitle,
            colorResOrAttr = R.attr.primary_color,
            position = position
        )
    }
}
