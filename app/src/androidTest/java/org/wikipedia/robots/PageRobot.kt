package org.wikipedia.robots

import android.app.Activity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
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
        onView(allOf(withId(R.id.buttonView))).inRoot(withDecorView(not(Matchers.`is`(activity.window.decorView))))
            .perform(click())
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

    fun swipeLeft() = apply {
        swipeLeft(R.id.pager)
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

    fun swipeDownOnTheWebView() {
        swipeDownOnTheWebView(R.id.page_contents_container)
    }
}
