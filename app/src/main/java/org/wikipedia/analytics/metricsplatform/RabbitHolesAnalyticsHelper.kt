package org.wikipedia.analytics.metricsplatform

import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object RabbitHolesAnalyticsHelper {
    val abcTest = RabbitHolesABCTest()

    private val enabledCountries = listOf(
        // sub-saharan africa
        "AO", "BJ", "BW", "IO", "BF", "BI", "CV", "CM", "CF", "TD", "KM", "CG", "IC", "CD", "DJ", "GQ", "ER",
        "SZ", "ET", "GA", "GM", "GH", "GN", "GW", "KE", "LS", "LR", "MG", "MW", "ML", "MR", "MU", "YT", "MZ",
        "NA", "NE", "NG", "RE", "RW", "SH", "ST", "SN", "SC", "SL", "SO", "ZA", "SS", "TG", "UG", "TZ", "ZM",
        "ZW",
        // south asia
        "IN", "PK", "BD", "LK", "MV", "NP", "BT", "AF"
    )

    val rabbitHolesEnabled get() = ReleaseUtil.isPreBetaRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    LocalDate.now() <= LocalDate.of(2024, 12, 31))
}
