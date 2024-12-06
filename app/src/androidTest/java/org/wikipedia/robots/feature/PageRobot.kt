package org.wikipedia.robots.feature

import android.annotation.SuppressLint
import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.DriverAtoms.webScrollIntoView
import androidx.test.espresso.web.webdriver.Locator
import org.wikipedia.R
import org.wikipedia.base.AssertJavascriptAction
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

    fun openPreviewLinkInNewTab() = apply {
        clickOnDisplayedView(R.id.link_preview_secondary_button)
        delay(TestConfig.DELAY_MEDIUM)
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

    fun navigateUp() = apply {
        clickOnDisplayedViewWithContentDescription("Navigate up")
        delay(TestConfig.DELAY_SHORT)
    }

    private fun assertElementVisibility(elementSelector: String, isVisible: Boolean) {
        onView(withId(R.id.page_web_view))
            .perform(AssertJavascriptAction("(function() { return document.querySelector(\"$elementSelector\").checkVisibility() })();", isVisible.toString()))
    }

    fun verifyPreviewDialogAppears() = apply {
        checkViewExists(R.id.link_preview_title)
        delay(TestConfig.DELAY_SHORT)
    }

    fun scrollToCollapsingTables() = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, ".pcs-table-infobox"))
            .perform(webScrollIntoView())
        delay(TestConfig.DELAY_MEDIUM)
    }

    @SuppressLint("CheckResult")
    fun verifyTableIsCollapsed() = apply {
        // checking if this class name exists
        // tried multiple methods but was not able to check the style for the collapsed/expanded
        // state of the table so instead using this className which is used when table is
        // collapsed
        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "pcs-collapse-table-expanded"))
    }

    @SuppressLint("CheckResult")
    fun verifyTableIsExpanded() = apply {
        // checking if this class name exists
        // tried multiple methods but was not able to check the style for the collapsed/expanded
        // state of the table so instead using this className which is used when table is
        // expanded
        onWebView()
            .withElement(findElement(Locator.CLASS_NAME, "pcs-collapse-table-collapsed"))
    }

    fun assertEditPencilVisibility(isVisible: Boolean) = apply {
        assertElementVisibility("a[data-id='0'].pcs-edit-section-link", isVisible)
    }

    fun assertCollapsingTableIsVisible(isVisible: Boolean) = apply {
        assertElementVisibility(".pcs-collapse-table-content", isVisible)
    }
}
