package org.wikipedia.robots.feature

import BaseRobot
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import org.hamcrest.Matchers.allOf
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.base.TestConfig

class MoreMenuRobot : BaseRobot() {

    fun verifyAllNonLoginItemsExists() = apply {
        verify.viewExists(R.id.main_drawer_places_container)
        verify.viewExists(R.id.main_drawer_settings_container)
        verify.viewExists(R.id.main_drawer_donate_container)
        verify.viewExists(R.id.main_drawer_login_button)
    }

    fun verifyAllLoginItemsExists() = apply {
        if (AccountUtil.isTemporaryAccount) {
            verify.viewExists(R.id.main_drawer_temp_account_container)
        }
        verify.viewExists(R.id.main_drawer_contribs_container)
        verify.viewExists(R.id.main_drawer_talk_container)
        verify.viewExists(R.id.main_drawer_watchlist_container)
        verify.viewWithIdIsNotVisible(R.id.main_drawer_login_button)
    }

    fun clickPlaces() = apply {
        click.onViewWithId(R.id.main_drawer_places_container)
    }

    fun clickSettings() = apply {
        click.onViewWithId(R.id.main_drawer_settings_container)
    }

    fun clickDonate() = apply {
        click.onViewWithId(R.id.main_drawer_donate_container)
    }

    fun clickLogin() = apply {
        click.onViewWithId(R.id.main_drawer_login_button)
    }

    fun clickUserContributions() = apply {
        click.onViewWithId(R.id.main_drawer_contribs_container)
    }

    fun clickTalk() = apply {
        click.onViewWithId(R.id.main_drawer_talk_container)
        delay(TestConfig.DELAY_MEDIUM)
    }

    fun clickWatchList() = apply {
        click.onViewWithId(R.id.main_drawer_watchlist_container)
    }

    fun verifyPlacesIsAccessible() = apply {
        verify.viewExists(R.id.mapViewButton)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyUserContributionIsAccessible() = apply {
        verify.viewExists(R.id.user_contrib_recycler)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyUserTalkIsAccessible() = apply {
        verify.viewExists(R.id.talkNewTopicButton)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyWatchListIsAccessible() = apply {
        verify.viewExists(R.id.watchlistRecyclerView)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyDonateFlowIsAccessible(context: Context) = apply {

        try {
            val customTabIntentMatcher = allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(context.getString(R.string.donate_url,
                    WikipediaApp.instance.languageState.systemLanguageCode, BuildConfig.VERSION_NAME))
            )
            intended(customTabIntentMatcher)
        } catch (e: AssertionError) {
            Log.e("MoreMenuRobotDonate: ", "no google pay")
            verify.viewExists(R.id.gPayTitle)
        }
    }
}
