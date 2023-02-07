package org.wikipedia.pageobjects
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import android.widget.TextView
import androidx.test.espresso.*
import java.util.concurrent.TimeoutException

open class BasePage {

    fun waitUntilElementsAreDisplayed(
        viewMatcher: Matcher<View>,
        timeout: Int = 1000
    ) {
        waitForCondition({ viewExists(viewMatcher) }, timeout.toLong())
    }

    private fun waitForCondition(condition: () -> Boolean, timeoutMillis: Long) {
        var totalTime: Long = 0

        do {
            if(condition()) {
                break
            }

            val step:Long = 250
            Thread.sleep(step)
            totalTime += step

            if(totalTime > timeoutMillis) {
                throw TimeoutException("Condition timed out")
            }
        } while (true)
    }

    fun viewExists(viewMatcher: Matcher<View>): Boolean {
        return try {
            onView(viewMatcher).check(matches(isDisplayed()))
            true
        } catch (e: NoMatchingViewException) {
            false
        }
    }

    fun getText(matcher: ViewInteraction): String {
        var text = String()
        matcher.perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "Text of the view"
            }

            override fun perform(uiController: UiController, view: View) {
                val tv = view as TextView
                text = tv.text.toString()
            }
        })
        return text
    }
}
