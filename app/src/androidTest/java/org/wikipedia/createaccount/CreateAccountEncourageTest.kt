package org.wikipedia.createaccount

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.wikipedia.R
import org.wikipedia.TestLogRule
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import org.wikipedia.history.HistoryEntry
import org.wikipedia.main.MainActivity
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class CreateAccountEncourageTest {
    private val composeTestRule = createComposeRule()
    private val permissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant()
    }

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(TestLogRule())
        .around(permissionRule)
        .around(composeTestRule)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private var mainActivityScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() = runBlocking {
        SharedPreferenceCookieManager.instance.clearAllCookies()
        AccountUtil.removeAccount()
        assertFalse(AccountUtil.isLoggedIn)

        AppDatabase.instance.historyEntryDao().deleteAll()
        Prefs.createAccountEncourageImpressions = 0
        Prefs.createAccountEncourageLastImpressionDate = ""
        Prefs.isInitialOnboardingEnabled = false
        Prefs.isExploreFeedUpdatePromptShown = true
        Prefs.readingChallengeOnboardingShown = true
        Prefs.isYearInReviewEnabled = false
        Prefs.yearInReviewVisited = true
        Prefs.queueLoggedOutInBackgroundDialog = false
        Prefs.showOneTimeCustomizeToolbarTooltip = false
    }

    @After
    fun tearDown() = runBlocking {
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .forEach(Activity::finish)
        }
        closeMainActivity()
        AppDatabase.instance.historyEntryDao().deleteAll()
        SharedPreferenceCookieManager.instance.clearAllCookies()
        AccountUtil.removeAccount()
        Prefs.createAccountEncourageImpressions = 0
        Prefs.createAccountEncourageLastImpressionDate = ""
    }

    @Test
    fun singleReadingDayDoesNotShowCreateAccountEncouragement() {
        runBlocking {
            insertReadingDay(LocalDate.now().minusDays(8))
        }

        launchMainActivity()
        waitForResumedActivity(MainActivity::class.java)
        assertActivityDoesNotResume(CreateAccountEncourageActivity::class.java)
        assertEquals(0, Prefs.createAccountEncourageImpressions)
    }

    @Test
    fun nearbyReadingDaysStillQualifyWhenAppIsOpenedAfterFourteenDays() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(7))
        insertReadingDay(today.minusDays(8))

        assertTrue(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun readingDaysExactlyFourteenDaysApartDoNotQualify() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(21))
        insertReadingDay(today.minusDays(7))

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun readingDaysThirteenDaysApartQualify() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(20))
        insertReadingDay(today.minusDays(7))

        assertTrue(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun readingDaysLessThanAWeekOldDoNotQualify() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(6))
        insertReadingDay(today.minusDays(1))

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun multipleReadsOnTheSameDayDoNotQualify() = runBlocking {
        val readingDate = LocalDate.now().minusDays(8)
        insertReadingDay(readingDate, articleSuffix = "first")
        insertReadingDay(readingDate, articleSuffix = "second")

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun readingDaysOutsideTheHistoryCutoffDoNotQualify() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(367))
        insertReadingDay(today.minusDays(366))

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun onlyTheEarlierReadingDayOutsideTheHistoryCutoffDoesNotQualify() = runBlocking {
        // The two days are close enough together to qualify, but the earlier one falls outside
        // the history cutoff, so it never reaches the pairing and the later one is left alone.
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(370))
        insertReadingDay(today.minusDays(360))

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun loggedInUserDoesNotQualifyDespiteQualifyingReadingHistory() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(8))
        insertReadingDay(today.minusDays(7))
        assertTrue(CreateAccountEncourageViewModel.shouldShow())

        addPermanentAccount()
        assertTrue(AccountUtil.isLoggedIn)
        assertFalse(AccountUtil.isTemporaryAccount)

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun temporaryAccountStillQualifies() = runBlocking {
        val today = LocalDate.now()
        insertReadingDay(today.minusDays(8))
        insertReadingDay(today.minusDays(7))

        setTemporaryAccountCookie()
        assertTrue(AccountUtil.isLoggedIn)
        assertTrue(AccountUtil.isTemporaryAccount)

        assertTrue(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun debugImpressionCountQualifiesWithoutAnyReadingHistory() = runBlocking {
        Prefs.createAccountEncourageImpressions = -1

        assertTrue(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun oneReturnReadingDayAfterMaybeLaterDoesNotQualify() = runBlocking {
        val today = LocalDate.now()
        Prefs.createAccountEncourageImpressions = 1
        Prefs.createAccountEncourageLastImpressionDate = today.minusDays(3).toString()
        insertReadingDay(today.minusDays(2))

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun invalidLastImpressionDateDoesNotQualifyForAnotherImpression() = runBlocking {
        Prefs.createAccountEncourageImpressions = 1
        Prefs.createAccountEncourageLastImpressionDate = "invalid"

        assertFalse(CreateAccountEncourageViewModel.shouldShow())
    }

    @Test
    fun qualifyingReadingHistoryShowsCreateAccountEncouragementAgainAfterMaybeLater() {
        val today = LocalDate.now()
        runBlocking {
            insertReadingDay(today.minusDays(8))
            insertReadingDay(today.minusDays(7))
        }

        launchMainActivity()
        assertEncouragementIsDisplayed()
        assertEquals(1, Prefs.createAccountEncourageImpressions)
        assertEquals(today.toString(), Prefs.createAccountEncourageLastImpressionDate)

        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_maybe_later))
            .performClick()
        waitForResumedActivity(MainActivity::class.java)
        assertEquals(1, Prefs.createAccountEncourageImpressions)

        launchMainActivity()
        waitForResumedActivity(MainActivity::class.java)
        assertActivityDoesNotResume(CreateAccountEncourageActivity::class.java)

        closeMainActivity()
        Prefs.createAccountEncourageLastImpressionDate = today.minusDays(3).toString()
        runBlocking {
            insertReadingDay(today.minusDays(2))
            insertReadingDay(today.minusDays(1))
        }

        launchMainActivity()
        assertEncouragementIsDisplayed()
        assertEquals(2, Prefs.createAccountEncourageImpressions)

        composeTestRule.onNodeWithText(context.getString(R.string.onboarding_maybe_later))
            .performClick()
        waitForResumedActivity(MainActivity::class.java)

        launchMainActivity()
        waitForResumedActivity(MainActivity::class.java)
        assertActivityDoesNotResume(CreateAccountEncourageActivity::class.java)
        assertEquals(2, Prefs.createAccountEncourageImpressions)
    }

    @Test
    fun closePermanentlyDismissesCreateAccountEncouragement() {
        val today = LocalDate.now()
        runBlocking {
            insertReadingDay(today.minusDays(8))
            insertReadingDay(today.minusDays(7))
        }

        launchMainActivity()
        assertEncouragementIsDisplayed()

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.dialog_close_description)
        ).performClick()
        waitForResumedActivity(MainActivity::class.java)
        assertEquals(100, Prefs.createAccountEncourageImpressions)

        launchMainActivity()
        waitForResumedActivity(MainActivity::class.java)
        assertActivityDoesNotResume(CreateAccountEncourageActivity::class.java)
        assertEquals(100, Prefs.createAccountEncourageImpressions)
    }

    private fun launchMainActivity() {
        closeMainActivity()
        mainActivityScenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
    }

    private fun closeMainActivity() {
        mainActivityScenario?.close()
        mainActivityScenario = null
    }

    private fun assertEncouragementIsDisplayed() {
        val title = context.getString(R.string.create_account_encourage_title)
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    private fun waitForResumedActivity(activityClass: Class<out Activity>) {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            isActivityResumed(activityClass)
        }
    }

    private fun assertActivityDoesNotResume(
        activityClass: Class<out Activity>,
        durationMillis: Long = 2_000
    ) {
        val deadline = SystemClock.elapsedRealtime() + durationMillis
        while (SystemClock.elapsedRealtime() < deadline) {
            assertFalse(isActivityResumed(activityClass))
            SystemClock.sleep(50)
        }
    }

    private fun isActivityResumed(activityClass: Class<out Activity>): Boolean {
        var isResumed = false
        instrumentation.runOnMainSync {
            isResumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .any(activityClass::isInstance)
        }
        return isResumed
    }

    private fun addPermanentAccount() {
        AccountManager.get(context).addAccountExplicitly(
            Account(PERMANENT_ACCOUNT_NAME, AccountUtil.accountType()), "password", null
        )
    }

    private fun setTemporaryAccountCookie() {
        SharedPreferenceCookieManager.instance.saveFromResponse(
            "https://en.wikipedia.org/".toHttpUrl(),
            listOf(
                Cookie.Builder()
                    .name(CENTRALAUTH_USER_COOKIE_NAME)
                    .value(TEMPORARY_ACCOUNT_NAME)
                    .domain("wikipedia.org")
                    .expiresAt(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))
                    .build()
            )
        )
    }

    private suspend fun insertReadingDay(date: LocalDate, articleSuffix: String = "") {
        val timestamp = date.atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
        AppDatabase.instance.historyEntryDao().insertEntry(
            HistoryEntry(
                authority = "en.wikipedia.org",
                lang = "en",
                apiTitle = "Article-${date.toEpochDay()}-$articleSuffix",
                displayTitle = "Article ${date.toEpochDay()} $articleSuffix",
                timestamp = Date.from(timestamp)
            )
        )
    }

    companion object {
        private const val CENTRALAUTH_USER_COOKIE_NAME = "centralauth_User"
        private const val PERMANENT_ACCOUNT_NAME = "CreateAccountEncourageTestUser"
        private const val TEMPORARY_ACCOUNT_NAME = "~2026-12345"
    }
}
