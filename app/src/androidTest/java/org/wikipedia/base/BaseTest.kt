package org.wikipedia.base

import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.wikipedia.main.MainActivity
import java.util.concurrent.TimeUnit

object TestConfig {
    const val DELAY_SHORT = 1L
    const val DELAY_MEDIUM = 2L
    const val DELAY_LARGE = 5L

    object Articles {
        const val SEARCH_TERM = "hopf fibration"
        const val ARTICLE_TITLE = "Hopf fibration"
        const val ARTICLE_TITLE_ESPANOL = "Fibración de Hopf"
    }
}

abstract class BaseTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    protected lateinit var activity: MainActivity
    protected lateinit var device: UiDevice

    @Before
    open fun setup() {
        activityScenarioRule.scenario.onActivity {
            activity = it
        }
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.SECONDS)
    }

    protected fun setDeviceOrientation(isLandscape: Boolean) {
        if (isLandscape) device.setOrientationRight() else device.setOrientationNatural()
        Thread.sleep(TestConfig.DELAY_MEDIUM)
    }

    @After
    open fun tearDown() {

    }
}