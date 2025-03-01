package org.wikipedia.base.actions

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import org.hamcrest.Matchers
import org.wikipedia.TestUtil
import org.wikipedia.TestUtil.delay
import org.wikipedia.base.TestConfig
import org.wikipedia.base.utils.ExecuteJavascriptAction

class WebActions {
    fun clickWebLink(linkTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "a[title='$linkTitle']"))
            .perform(webClick())
    }

    fun verifyH1Title(expectedTitle: String) = apply {
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "h1"))
            .check(
                WebViewAssertions.webMatches(
                    DriverAtoms.getText(),
                    Matchers.`is`(expectedTitle)
                )
            )
    }

    fun swipeDownOnTheWebView(@IdRes viewId: Int) {
        onView(withId(viewId)).perform(TestUtil.swipeDownWebView())
        delay(TestConfig.DELAY_LARGE)
    }

    fun scrollToImageInWebView(imageIndex: Int): ViewAction {
        val scrollScript = """
            (function findContentImages() {
                const contentImages = Array.from(document.querySelectorAll('img'))
                    .filter(img => img.complete && img.naturalWidth > 100 && img.naturalHeight > 100)
                if (contentImages.length > $imageIndex) {
                    contentImages[$imageIndex].scrollIntoView({ behavior: 'smooth', block: 'center' })
                    return 'success'
                }
                return 'image not found'
            })()
        """.trimIndent()
        return ExecuteJavascriptAction(scrollScript)
    }
}
