package org.wikipedia.robots.feature

import android.app.Activity
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import org.wikipedia.R
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class PageRobot : BaseRobot() {
    fun clickEditPencilAtTopOfArticle() = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[data-id='0'].pcs-edit-section-link"))
            .perform(webClick())
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickLink(linkTitle: String) = apply {
        clickWebLink(linkTitle)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyArticleTitle(expectedTitle: String) = apply {
        verifyH1Title(expectedTitle)
    }

    fun previewArticle() = apply {
        clickOnDisplayedView(R.id.link_preview_toolbar)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun openInNewTab() = apply {
        clickOnDisplayedView(R.id.link_preview_secondary_button)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyTabCount(count: String) = apply {
        checkWithTextIsDisplayed(R.id.tabsCountText, count)
    }

    fun dismissTooltip(activity: Activity) = apply {
        dismissTooltipIfAny(activity, viewId = R.id.buttonView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyHeaderViewWithLeadImage() = apply {
        checkViewExists(R.id.page_header_view)
    }

    fun clickLeadImage() = apply {
        clickOnDisplayedView(R.id.view_page_header_image)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickOverflowMenu(description: String) = apply {
        clickOnDisplayedViewWithContentDescription(description)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun visitImagePage() = apply {
        clicksOnDisplayedViewWithText(viewId = R.id.title, text = "Go to image page")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyLeadImageIsNotVisible() = apply {
        checkViewDoesNotExist(R.id.page_header_view)
    }

    fun swipePagerLeft() = apply {
        swipeLeft(R.id.pager)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun swipeLeftToShowTableOfContents() = apply {
        swipeLeft(R.id.page_web_view)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }

    fun goBackToGalleryView() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun goBackToOriginalArticle() = apply {
        goBack()
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun enableJavaScript() = apply {
        onWebView().forceJavascriptEnabled()
    }

    fun launchTabsScreen() = apply {
        clickOnDisplayedView(R.id.page_toolbar_button_tabs)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun createNewTabWithContentDescription(text: String) = apply {
        clickOnDisplayedViewWithContentDescription(text)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickOnPreviewTabInTheList(position: Int) = apply {
        clickRecyclerViewItemAtPosition(R.id.tabRecyclerView, position)
    }

    fun swipeDownOnTheWebView() = apply {
        swipeDownOnTheWebView(R.id.page_contents_container)
    }

    fun verifyTopMostItemInTableOfContentIs(text: String) = apply {
       checkViewWithIdAndText(viewId = R.id.page_toc_item_text, text)
    }

    fun swipeTableOfContentsAllTheWayToBottom() = apply {
        swipeUp(R.id.toc_list)
    }

    fun clickAboutThisArticleText() = apply {
        clickOnViewWithText("About this article")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun goToTalkPage() = apply {
        onWebView().withElement(findElement(Locator.CSS_SELECTOR, "a[title='View talk page']"))
            .perform(webClick())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickThirdTopic() = apply {
        clickRecyclerViewItemAtPosition(R.id.talkRecyclerView, 2)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun saveArticleToReadingList() = apply {
        clickOnViewWithId(R.id.page_save)
        delay(TestConfig.DELAY_SHORT)
    }

    fun openLanguageSelector() = apply {
        clickOnDisplayedViewWithIdAnContentDescription(R.id.page_language, "Language")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickLanguageListedAtFourthPosition() = apply {
        clickRecyclerViewItemAtPosition(R.id.langlinks_recycler, 3)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun openOverflowMenu() = apply {
        clickOnViewWithId(R.id.page_toolbar_button_show_overflow_menu)
        delay(TestConfig.DELAY_SHORT)
    }

    fun navigateBackToExploreFeed() = apply {
        clickOnViewWithText("Explore")
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickOnBookmarkIcon() = apply {
        clickOnViewWithId(R.id.page_save)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun removeArticleFromReadingList() = apply {
        clickOnViewWithText("Remove from Saved")
        delay(TestConfig.DELAY_LARGE)
    }
}
