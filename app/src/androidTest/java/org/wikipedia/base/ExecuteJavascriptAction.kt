package org.wikipedia.base

import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.util.HumanReadables
import org.hamcrest.Matcher
import java.util.concurrent.atomic.AtomicBoolean

class ExecuteJavascriptAction(private val script: String) : ViewAction, ValueCallback<String> {
    private val evaluateFinished = AtomicBoolean(false)

    override fun getConstraints(): Matcher<View> {
        return isAssignableFrom(WebView::class.java)
    }

    override fun getDescription(): String {
        return "Execute Javascript Action"
    }

    override fun perform(uiController: UiController, view: View) {
        uiController.loopMainThreadUntilIdle()

        val webView = view as WebView
        val exception = PerformException.Builder()
            .withActionDescription(this.description)
            .withViewDescription(HumanReadables.describe(view))

        webView.evaluateJavascript(script, this)

        val maxTime = System.currentTimeMillis() + 5000
        while (!evaluateFinished.get()) {
            if (System.currentTimeMillis() > maxTime) {
                throw exception
                    .withCause(RuntimeException("Evaluating Javascript timed out."))
                    .build()
            }
            uiController.loopMainThreadForAtLeast(50)
        }

        uiController.loopMainThreadForAtLeast(500)
    }

    override fun onReceiveValue(value: String?) {
        evaluateFinished.set(true)
    }
}
