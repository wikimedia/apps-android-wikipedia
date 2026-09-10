package org.wikipedia.yearinreview

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.GeoUtil

interface YearInReviewRepository {
    suspend fun getYearInReview(year: Int): YearInReviewSnapshot
}

class YearInReviewRepositoryImpl(
    private val restService: RestService = ServiceFactory.getRest(WikipediaApp.instance.wikiSite)
) : YearInReviewRepository {

    override suspend fun getYearInReview(year: Int): YearInReviewSnapshot {
        val remoteConfig = restService.getConfiguration().commonv1?.getYirForYear(year)
        val isDonationEligible = remoteConfig != null &&
                !remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty())

        return YearInReviewSnapshot(
            year = year,
            isDonationEligible = isDonationEligible
        )
    }
}
