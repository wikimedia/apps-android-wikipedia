package org.wikipedia.base.base

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.wikipedia.TestUtil.waitOnId
import org.wikipedia.base.actions.ClickActions
import org.wikipedia.base.actions.InputActions
import org.wikipedia.base.actions.ListActions
import org.wikipedia.base.actions.ScrollActions
import org.wikipedia.base.actions.SwipeActions
import org.wikipedia.base.actions.SystemActions
import org.wikipedia.base.actions.VerificationActions
import org.wikipedia.base.actions.WebActions
import java.util.concurrent.TimeUnit

abstract class BaseRobot {
    protected val click = ClickActions()
    protected val input = InputActions()
    protected val list = ListActions()
    protected val scroll = ScrollActions()
    protected val swipe = SwipeActions()
    protected val system = SystemActions()
    protected val verify = VerificationActions()
    protected val web = WebActions()

    protected fun delay(seconds: Long) {
        onView(isRoot()).perform(waitOnId(TimeUnit.SECONDS.toMillis(seconds)))
    }

    protected fun goBack() {
        pressBack()
    }
}
