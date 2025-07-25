package org.wikipedia.donate

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {

    const val MAX_INITIAL_PROMPTS = 5
    const val MAX_REMINDER_PROMPTS = 2

    // Users with a device location in Germany, France, Poland, or Philippines
    private val enabledCountries = listOf(
        "DE", "FR", "PL", "PH"
    )

    // Users with a primary language of German, English, French, or Polish
    private val enabledLanguages = listOf(
        "de", "fr", "pl", "en"
    )

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled get() = ReleaseUtil.isDevRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                    LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)
}
