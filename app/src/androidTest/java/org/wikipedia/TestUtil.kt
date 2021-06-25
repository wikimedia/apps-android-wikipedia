package org.wikipedia

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.*
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
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

    fun hasBackgroundColor(@ColorInt color: Int): Matcher<View> {
        return BackgroundColorMatcher(color)
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

    fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent) &&
                        view == parent.getChildAt(position)
            }
        }
    }

    fun setAirplaneMode(enabled: Boolean, delaySecAfter: Long = 1) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        /*
        The least hacky way, but no longer works since API 24:

        device.executeShellCommand("su 0 settings put global airplane_mode_on " + if (enabled) "1" else "0")
        device.executeShellCommand("su 0 am broadcast -a android.intent.action.AIRPLANE_MODE")
        */
        /*
        Extremely hacky:

        device.openNotification()
        Thread.sleep(2000)
        device.click(device.displayWidth * 90 / 100, device.displayHeight * 15 / 100)
        Thread.sleep(2000)
        device.pressBack()
        Thread.sleep(delaySecAfter * 1000)
        */

        // Slightly less hacky:
        device.executeShellCommand("am start -a android.settings.AIRPLANE_MODE_SETTINGS")
        Thread.sleep(2000)

        val obj = device.findObject(By.text("Airplane mode"))
        // get the parent container that is actually clickable
        var parent = obj
        while (!parent.isClickable) {
            parent = parent.parent
        }
        // look for the switch component and ascertain its state
        val switch = parent.findObject(By.checkable(true))
        if ((switch.isChecked && !enabled) || (!switch.isChecked && enabled)) {
            parent.click()
        }

        Thread.sleep(delaySecAfter * 1000)
        device.pressBack()
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

    internal class BackgroundColorMatcher(@ColorInt private val color: Int) : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("has background color of $color")
        }

        public override fun matchesSafely(view: View): Boolean {
            return view.background is ColorDrawable && (view.background as ColorDrawable).color == color
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
