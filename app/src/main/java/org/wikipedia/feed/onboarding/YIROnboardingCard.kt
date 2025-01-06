package org.wikipedia.feed.onboarding

import org.wikipedia.R
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.model.CardType
import org.wikipedia.util.GeoUtil
import java.time.LocalDate

class YIROnboardingCard(announcement: Announcement) : OnboardingCard(announcement) {

    override fun type(): CardType {
        return CardType.YEAR_IN_REVIEW_ANNOUNCEMENT
    }

    override fun shouldShow(): Boolean {
        return super.shouldShow() &&
                LocalDate.now() <= LocalDate.of(2025, 1, 31) &&
                !excludedCountries.contains(GeoUtil.geoIPCountry)
    }

    override fun prefKey(): Int {
        return R.string.preference_key_feed_yir_onboarding_card_enabled
    }

    private val excludedCountries = setOf("RU", "IR", "CN", "HK", "MO", "SA", "CU", "MM", "BY", "EG", "PS", "GN", "PK", "KH", "VN", "SD", "AE", "SY", "JO", "VE", "AF")
}
