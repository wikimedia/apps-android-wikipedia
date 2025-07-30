package org.wikipedia.donate

import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {

    private val enabledCountries = listOf(
        "IT"
    )

    private val enabledLanguages = listOf(
        "it", "en"
    )

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled get() = ReleaseUtil.isDevRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                    LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)
}

@Serializable
data class DonationReminderConfig(
    var isEnabled: Boolean = false,
    var initialPromptCount: Int = 0,
    var initialPromptDismissed: Boolean = false,
    var finalPromptCount: Int = 0,
    var finalPromptDismissed: Boolean = false,
    var promptLastSeen: Long = 0,
    var setupTimestamp: Long = 0,
    var articleVisit: Int = 0,
    var isSurveyShown: Boolean = false,
    var articleFrequency: Int = 0,
    var donateAmount: Int = 0
)
