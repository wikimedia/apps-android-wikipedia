package org.wikipedia.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.main.SmokeTest2.Companion.ARTICLE_TITLE
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.PageRobot
import org.wikipedia.robots.ThemeRobot

@LargeTest
@RunWith(AndroidJUnit4::class)
class PageTest : BaseTest<PageActivity>(
    PageActivity::class.java,
    {
        action = ACTION_LOAD_IN_CURRENT_TAB
        putExtra(EXTRA_HISTORYENTRY, FakeData.historyEntry)
        putExtra(Constants.ARG_TITLE, FakeData.historyEntry.title)
    }
) {
    private val pageRobot = PageRobot()
    private val themeRobot = ThemeRobot()

    @Test
    fun articlePageTest() {
        pageRobot
            .dismissTooltip(activity)
            .clickLink("3-sphere")
            .previewArticle()
            .clickLink("Sphere")
            .openInNewTab()
            .verifyTabCount("2")
            .goBackToOriginalArticle()
            .verifyHeaderViewWithLeadImage()
            .clickLeadImage()
            .swipeLeft()
            .clickOverflowMenu("More options")
            .visitImagePage()
            .goBackToGalleryView()
            .goBackToOriginalArticle()
            .enableJavaScript()
            .verifyArticleTitle(ARTICLE_TITLE)
        setDeviceOrientation(isLandscape = true)
        pageRobot
            .verifyLeadImageIsNotVisible()
            .verifyArticleTitle(ARTICLE_TITLE)
        setDeviceOrientation(isLandscape = false)
        themeRobot
            .toggleTheme()
            .switchOffMatchSystemTheme()
            .selectBlackTheme()
            .pressBack()
            .verifyBackgroundIsBlack()
            .goBackToLightTheme()
            .pressBack()
        pageRobot
            .launchTabsScreen()
            .createNewTabWithContentDescription(text = "New tab")
            .launchTabsScreen()
            .clickOnPreviewTabInTheList(1)
            .swipeDownOnTheWebView()
    }
}
