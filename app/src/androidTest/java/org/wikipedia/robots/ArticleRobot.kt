package org.wikipedia.robots

import android.app.Activity
import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import com.google.android.apps.common.testing.accessibility.framework.utils.contrast.Color
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.wikipedia.R
import org.wikipedia.TestUtil
import org.wikipedia.base.BaseRobot
import org.wikipedia.base.TestConfig

class ArticleRobot : BaseRobot() {
    fun clickLink(linkTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[title='$linkTitle']"))
            .perform(webClick())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun verifyArticleTitle(expectedTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "h1"))
            .check(WebViewAssertions.webMatches(DriverAtoms.getText(), Matchers.`is`(expectedTitle)))
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

    fun toggleTheme() = apply {
        clickOnDisplayedView(R.id.page_theme)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun switchOffMatchSystemTheme() = apply {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            checkViewDoesNotExist(R.id.theme_chooser_match_system_theme_switch)
        } else {
            scrollToViewAndClick(R.id.theme_chooser_match_system_theme_switch)
        }
        delay(TestConfig.DELAY_SHORT)
    }

    fun selectBlackTheme() = apply {
        scrollToViewAndClick(R.id.button_theme_black)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyBackgroundIsBlack() = apply {
        onView(withId(R.id.page_actions_tab_layout)).check(matches(TestUtil.hasBackgroundColor(Color.BLACK)))
    }

    fun goBackToLightTheme() = apply {
        clickWithId(R.id.page_theme)
        delay(TestConfig.DELAY_SHORT)
        scrollToViewAndClick(R.id.button_theme_light)
        delay(TestConfig.DELAY_MEDIUM)
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
}
