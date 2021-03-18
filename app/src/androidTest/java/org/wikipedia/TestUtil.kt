package org.wikipedia

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.util.concurrent.TimeUnit

object TestUtil {

    fun delay(sec: Long) {
        onView(isRoot()).perform(waitOnId(TimeUnit.SECONDS.toMillis(sec)))
    }

    fun withGrandparent(grandparentMatcher: Matcher<View>): Matcher<View> {
        return WithGrandparentMatcher(grandparentMatcher)
    }

    fun waitOnId(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "Wait a specified amount of time."
            }

            override fun perform(uiController: UiController?, view: View?) {
                uiController?.loopMainThreadForAtLeast(millis)
            }
        }
    }

    internal class WithGrandparentMatcher constructor(private val grandparentMatcher: Matcher<View>) : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("has grandparent matching: ")
            grandparentMatcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            return grandparentMatcher.matches(view.parent.parent)
        }
    }
}
