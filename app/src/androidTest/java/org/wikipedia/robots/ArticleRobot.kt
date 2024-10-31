package org.wikipedia.robots

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
import org.wikipedia.main.MainActivity

class ArticleRobot : BaseRobot() {

    fun clickLink(linkTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[title='$linkTitle']"))
            .perform(webClick())
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun dismissTooltip(activity: MainActivity) = apply {
        onView(allOf(withId(R.id.buttonView))).inRoot(withDecorView(not(Matchers.`is`(activity.window.decorView))))
            .perform(click())
        delay(TestConfig.DELAY_SHORT)
    }
}
