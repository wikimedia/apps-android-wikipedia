package org.wikipedia.donate.donationreminder

import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val currencyAmountPresets = mapOf(
        "IT" to listOf(1f, 2f, 3f)
    )

    val defaultReadFrequencyOptions = listOf(5, 10, 15)

    fun getDonationReminderSubmittedFormDate(): String {
        val timeStamp = Prefs.donationReminderConfig.setupTimestamp
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timeStamp),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
        return localDateTime.format(formatter)
    }
}

@Serializable
data class DonationReminderConfig(
    val isEnabled: Boolean = false,
    val initialPromptCount: Int = 0,
    val initialPromptDismissed: Boolean = false,
    val finalPromptCount: Int = 0,
    val finalPromptDismissed: Boolean = false,
    val promptLastSeen: Long = 0,
    val setupTimestamp: Long = 0,
    val articleVisit: Int = 0,
    val isSurveyShown: Boolean = false,
    val articleFrequency: Int = 0,
    val donateAmount: Float = 0f
)
