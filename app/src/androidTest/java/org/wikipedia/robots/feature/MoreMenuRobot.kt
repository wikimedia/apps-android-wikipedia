package org.wikipedia.robots.feature

import BaseRobot
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil

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
        verify.viewDoesNotExist(R.id.main_drawer_login_button)
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
    }

    fun clickWatchList() = apply {
        click.onViewWithId(R.id.main_drawer_watchlist_container)
    }

    fun verifyPlacesIsAccessible() = apply {
        verify.viewExists(R.id.mapViewButton)
    }

    fun verifyUserContributionIsAccessible() = apply {
        verify.viewExists(R.id.user_contrib_recycler)
    }

    fun verifyUserTalkIsAccessible() = apply {
        verify.viewExists(R.id.talkNewTopicButton)
    }

    fun verifyWatchListIsAccessible() = apply {
        verify.viewExists(R.id.watchlistRecyclerView)
    }
}
