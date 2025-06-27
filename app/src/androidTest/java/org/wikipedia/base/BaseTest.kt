package org.wikipedia.base

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.wikipedia.TestLogRule
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import java.util.concurrent.TimeUnit

object TestConfig {
    const val DELAY_SHORT = 1L
    const val DELAY_MEDIUM = 2L
    const val DELAY_LARGE = 5L
    const val DELAY_SWIPE_TO_REFRESH = 8L
    const val SEARCH_TERM = "hopf fibration"
    const val SEARCH_TERM2 = "world cup"
    const val ARTICLE_TITLE = "Hopf fibration"
    const val ARTICLE_TITLE_ESPANOL = "Fibración de Hopf"
    const val TEST_WIKI_URL_APPLE = "https://en.wikipedia.org/wiki/Apple"
    const val ARTICLE_TITLE_WORLD_CUP = "World cup"
}

data class DataInjector(
    val isInitialOnboardingEnabled: Boolean = false,
    val overrideEditsContribution: Int? = null,
    val intentBuilder: (Intent.() -> Unit)? = null,
    val showOneTimeCustomizeToolbarTooltip: Boolean = false,
    val readingListShareTooltipShown: Boolean = true,
    val otdEntryDialogShown: Boolean = true
)

abstract class BaseTest<T : AppCompatActivity>(
    activityClass: Class<T>,
    dataInjector: DataInjector = DataInjector()
) {
    @get:Rule
    val testLogRule = TestLogRule()

    @get:Rule
    var activityScenarioRule: ActivityScenarioRule<T>

    @get:Rule
    var composeTestRule = createComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS
    )

    protected lateinit var activity: T
    protected lateinit var device: UiDevice
    protected var context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        val intent = Intent(context, activityClass)
        activityScenarioRule = ActivityScenarioRule(intent)
        Prefs.isInitialOnboardingEnabled = dataInjector.isInitialOnboardingEnabled
        Prefs.showOneTimeCustomizeToolbarTooltip = dataInjector.showOneTimeCustomizeToolbarTooltip
        Prefs.readingListShareTooltipShown = dataInjector.readingListShareTooltipShown
        Prefs.otdEntryDialogShown = dataInjector.otdEntryDialogShown
        dataInjector.overrideEditsContribution?.let {
            Prefs.overrideSuggestedEditContribution = it
        }
        dataInjector.intentBuilder?.let {
            val newIntent = Intent(context, activityClass).apply(it)
            activityScenarioRule = ActivityScenarioRule(newIntent)
        }
    }

    @Before
    open fun setup() {
        Intents.init()
        ComposeTestManager.setComposeTestRule(composeTestRule)
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.SECONDS)
        activityScenarioRule.scenario.onActivity {
            activity = it
        }
        WikipediaApp.instance.languageState.let {
            it.removeAppLanguageCodes(it.appLanguageCodes.filter { it != "en" })
        }
    }

    protected fun setDeviceOrientation(isLandscape: Boolean) {
        if (isLandscape) device.setOrientationRight() else device.setOrientationNatural()
        Thread.sleep(TestConfig.DELAY_MEDIUM)
    }

    @After
    open fun tearDown() {
        Intents.release()
    }
}
