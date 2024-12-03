package org.wikipedia.test.loggedoutuser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.FakeData
import org.wikipedia.base.BaseTest
import org.wikipedia.base.TestConfig.ARTICLE_TITLE
import org.wikipedia.base.TestConfig.ARTICLE_TITLE_ESPANOL
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.robots.ThemeRobot
import org.wikipedia.robots.feature.PageRobot

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
    fun startArticlePageTest() {
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
            .swipePagerLeft()
            .clickOverflowMenu("More options")
            .visitImagePage()
            .goBackToGalleryView()
            .goBackToOriginalArticle()
            .enableJavaScript()
            .verifyArticleTitle(ARTICLE_TITLE)
            .assertEditPencilVisible()
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
            .verifyArticleTitle(ARTICLE_TITLE)
            .swipeLeftToShowTableOfContents()
            .verifyTopMostItemInTableOfContentIs(ARTICLE_TITLE)
            .swipeTableOfContentsAllTheWayToBottom()
            .clickAboutThisArticleText()
            .goToTalkPage()
            .clickThirdTopic()
            .pressBack() // goes back to talk interface
            .pressBack() // goes back to article screen
            .saveArticleToReadingList()
            .openLanguageSelector()
            .clickLanguageListedAtFourthPosition()
            .verifyArticleTitle(ARTICLE_TITLE_ESPANOL)
            .openOverflowMenu()
    }
}
