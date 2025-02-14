package org.wikipedia.base.utils

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

class AssertJavascriptAction(val script: String, val expectedResult: String) : ViewAction, ValueCallback<String> {
    private var result: String? = null
    private val evaluateFinished = AtomicBoolean(false)
    private val exception = PerformException.Builder()
        .withActionDescription(this.description)

    override fun getConstraints(): Matcher<View> {
        return isAssignableFrom(WebView::class.java)
    }

    override fun getDescription(): String {
        return "Evaluate Javascript"
    }

    override fun perform(uiController: UiController, view: View) {
        uiController.loopMainThreadUntilIdle()

        val webView = view as WebView
        exception.withViewDescription(HumanReadables.describe(view))

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
    }

    override fun onReceiveValue(value: String) {
        evaluateFinished.set(true)
        val cleanValue = value.trim('"')
        if (cleanValue != expectedResult) {
            throw exception
                .withCause(RuntimeException("Expected: $expectedResult, but got: $value"))
                .build()
        }
    }
}
