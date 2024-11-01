package org.wikipedia.main

import android.location.Location
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.Constants
import org.wikipedia.base.BaseTest
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.main.SmokeTest2.Companion.ARTICLE_TITLE
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageActivity.Companion.ACTION_LOAD_IN_CURRENT_TAB
import org.wikipedia.page.PageActivity.Companion.EXTRA_HISTORYENTRY
import org.wikipedia.page.PageTitle
import org.wikipedia.robots.ArticleRobot


@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleTest: BaseTest<PageActivity>(
    PageActivity::class.java,
    {
        action = ACTION_LOAD_IN_CURRENT_TAB
        putExtra(EXTRA_HISTORYENTRY, FakeData.historyEntry)
        putExtra(Constants.ARG_TITLE, FakeData.historyEntry.title)
    }
) {
    private val articleRobot = ArticleRobot()

    @Test
    fun articlePageTest() {
        articleRobot
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
        articleRobot
            .verifyLeadImageIsNotVisible()
            .verifyArticleTitle(ARTICLE_TITLE)
        setDeviceOrientation(isLandscape = false)
        articleRobot
            .toggleTheme()
            .switchOffMatchSystemTheme()
            .selectBlackTheme()
            .pressBack()
            .verifyBackgroundIsBlack()
            .goBackToLightTheme()
            .pressBack()
    }
}

object FakeData {
    val title = PageTitle(
        _displayText = "Hopf_fibration",
        _text = "Hopf fibration",
        description = "Fiber bundle of the 3-sphere over the 2-sphere, with 1-spheres as fibers",
        thumbUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b9/Hopf_Fibration.png/320px-Hopf_Fibration.png",
        wikiSite = WikiSite(
            uri = Uri.parse("https://en.wikipedia.org")
        )
    )
    val inNewTab = false
    val position = 0
    val location: Location? = null
    val historyEntry = HistoryEntry(title, HistoryEntry.SOURCE_SEARCH)
}