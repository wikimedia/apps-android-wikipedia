package org.wikipedia.pageobjects
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import android.widget.TextView
import androidx.test.espresso.*


open class BasePage {
    var text= "Bitcoin"

    fun viewExists(viewMatcher: Matcher<View>): Boolean {
        return try {
            onView(viewMatcher).check(matches(isDisplayed()))
            true
        } catch (e: PerformException) {
            false
        } catch (e: NoMatchingViewException) {
            false
        }
    }

    fun invokeText(matcher: ViewInteraction): String {
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
    //https://www.testrisk.com/2019/12/getting-text-of-element-in-espresso.html
}