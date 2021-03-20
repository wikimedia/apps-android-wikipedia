package org.wikipedia

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.*
import androidx.test.espresso.matcher.ViewMatchers.isRoot
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

    fun isNotVisible(): Matcher<View> {
        return IsNotVisibleMatcher()
    }

    fun swipeDownWebView(): ViewAction {
        return ViewActions.actionWithAssertions(
                GeneralSwipeAction(Swipe.FAST, TranslatedCoordinatesProvider(GeneralLocation.TOP_CENTER, 0f, 0.25f),
                GeneralLocation.BOTTOM_CENTER,
                Press.FINGER))
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

    internal class IsNotVisibleMatcher : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("is not displayed on the screen to the user")
        }

        public override fun matchesSafely(view: View): Boolean {
            return (view.visibility != View.VISIBLE)
        }
    }

    internal class TranslatedCoordinatesProvider(private val coordinatesProvider: CoordinatesProvider, val dx: Float, private val dy: Float) : CoordinatesProvider {
        override fun calculateCoordinates(view: View): FloatArray {
            val xy = coordinatesProvider.calculateCoordinates(view)
            xy[0] += dx * view.width
            xy[1] += dy * view.height
            return xy
        }
    }
}
