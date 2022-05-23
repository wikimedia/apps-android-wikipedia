package org.wikipedia.feed.onboarding

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.model.CardType

class CustomizeOnboardingCard(announcement: Announcement) : OnboardingCard(announcement) {

    override fun type(): CardType {
        return CardType.ONBOARDING_CUSTOMIZE_FEED
    }

    override fun shouldShow(): Boolean {
        return super.shouldShow() && WikipediaApp.instance.isOnline
    }

    override fun prefKey(): Int {
        return R.string.preference_key_feed_customize_onboarding_card_enabled
    }
}
