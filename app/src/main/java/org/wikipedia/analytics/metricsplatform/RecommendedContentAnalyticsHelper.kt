package org.wikipedia.analytics.metricsplatform

import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil

// TODO: integrate this with ArticleLinkPreviewInteraction
class RecommendedContentAnalyticsHelper {

    companion object {
        private const val RECOMMENDED_CONTENT = "recommendedContent"
        val abcTest = RecommendedContentABCTest()

        private val enableCountries = listOf(
            // sub-saharan africa
            "AO", "BJ", "BW", "IO", "BF", "BI", "CV", "CM", "CF", "TD", "KM", "CG", "IC", "CD", "DJ", "GQ", "ER",
            "SZ", "ET", "GA", "GM", "GH", "GN", "GW", "KE", "LS", "LR", "MG", "MW", "ML", "MR", "YT", "MZ", "NA",
            "NE", "NG", "RE", "RW", "SH", "ST", "SN", "SC", "SL", "SO", "ZA", "SS", "TG", "UG", "TZ", "ZM", "ZW",
            // south asia
            "IN", "PK", "BD", "LK", "MU", "MV", "NP", "BT", "AF"
        )

        fun recommendedContentEnabled(): Boolean {
            return ReleaseUtil.isPreBetaRelease || enableCountries.contains(GeoUtil.geoIPCountry.orEmpty())
        }
    }
}
