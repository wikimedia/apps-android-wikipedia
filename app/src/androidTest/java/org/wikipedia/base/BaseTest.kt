package org.wikipedia.base

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.rules.RuleChain
import org.wikipedia.TestLogRule

/**
 * Base class for instrumented UI tests that launch an Activity.
 *
 * Launches [T] with a configurable [Intent]. Use Espresso to interact with Android Views and
 * [composeTestRule] to interact with Compose content. Both APIs can be used for hybrid screens.
 *
 * @param T Activity type under test. It must extend [ComponentActivity].
 * @param activityClass Class of the Activity to launch.
 * @param configureIntent Configures the Intent used to launch the Activity.
 */
abstract class BaseTest<T : ComponentActivity>(
    activityClass: Class<T>,
    configureIntent: Intent.() -> Unit = {}
) {
    protected val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    // Launches the Activity before each test, manages its lifecycle, and closes it afterward.
    private val activityScenarioRule = ActivityScenarioRule<T>(
        Intent(targetContext, activityClass).apply(configureIntent)
    )

    protected val composeTestRule = AndroidComposeTestRule(
        activityRule = activityScenarioRule,
        activityProvider = ActivityScenarioRule<T>::launchedActivity
    )

    private val testLogRule = TestLogRule()

    @get:Rule
    val activityTestRules: RuleChain = RuleChain
        .outerRule(testLogRule)
        .around(composeTestRule)

    protected val activity: T
        get() = composeTestRule.activity
}

/**
 * Returns the currently launched Activity for [AndroidComposeTestRule]'s activity provider.
 */
private fun <T : ComponentActivity> ActivityScenarioRule<T>.launchedActivity(): T {
    var launchedActivity: T? = null
    scenario.onActivity { launchedActivity = it }
    return checkNotNull(launchedActivity)
}
