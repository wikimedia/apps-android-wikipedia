package org.wikipedia.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.wikipedia.settings.Prefs
import java.util.concurrent.TimeUnit

object TestConfig {
    const val DELAY_SHORT = 1L
    const val DELAY_MEDIUM = 2L
    const val DELAY_LARGE = 5L
    const val DELAY_SWIPE_TO_REFRESH = 8L
    const val SEARCH_TERM = "hopf fibration"
    const val ARTICLE_TITLE = "Hopf fibration"
    const val ARTICLE_TITLE_ESPANOL = "Fibración de Hopf"
}

data class DataInjector(
    val isInitialOnboardingEnabled: Boolean = false
)

abstract class BaseTest<T : AppCompatActivity> {
    @get:Rule
    val activityScenarioRule: ActivityScenarioRule<T>

    protected lateinit var activity: T
    protected lateinit var device: UiDevice

    constructor(activityClass: Class<T>) {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, activityClass)
        activityScenarioRule = ActivityScenarioRule(intent)
    }

    constructor(activityClass: Class<T>, dataInjector: DataInjector) {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, activityClass)
        activityScenarioRule = ActivityScenarioRule(intent)
        Prefs.isInitialOnboardingEnabled = dataInjector.isInitialOnboardingEnabled
    }

    @Before
    open fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.SECONDS)
        activityScenarioRule.scenario.onActivity {
            activity = it
        }
    }

    protected fun setDeviceOrientation(isLandscape: Boolean) {
        if (isLandscape) device.setOrientationRight() else device.setOrientationNatural()
        Thread.sleep(TestConfig.DELAY_MEDIUM)
    }

    @After
    open fun tearDown() {
        // @TODO
    }
}
