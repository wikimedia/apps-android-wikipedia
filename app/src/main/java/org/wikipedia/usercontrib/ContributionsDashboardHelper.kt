package org.wikipedia.usercontrib

import org.wikipedia.WikipediaApp
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

class ContributionsDashboardHelper {

    companion object {

        private val enabledCountries = listOf(
            "FR", "NL", "IT", "JP"
        )

        private val enabledLanguages = listOf(
            "fr", "nl", "en", "it", "ja"
        )

        val contributionsDashboardEnabled get() = ReleaseUtil.isPreBetaRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2024, 12, 2))
    }
}
