package org.wikipedia.dataclient.donate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Request
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.donate.DonationResult
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import java.time.LocalDateTime

object CampaignCollection {

    private const val CAMPAIGN_VERSION = 2
    private const val CAMPAIGN_PLATFORM = "Android"

    private const val CAMPAIGNS_URL = "https://donate.wikimedia.org/wiki/MediaWiki:AppsCampaignConfig.json?action=raw"
    private const val CAMPAIGNS_URL_DEBUG = "https://test.wikipedia.org/wiki/MediaWiki:AppsCampaignConfig.json?action=raw"

    private const val DAYS_TO_HIDE_AFTER_DONATION = 250L

    suspend fun getActiveCampaigns(): List<Campaign> {
        val campaignList = mutableListOf<Campaign>()

        withContext(Dispatchers.IO) {
            val url = if (Prefs.announcementDebugUrl) CAMPAIGNS_URL_DEBUG else CAMPAIGNS_URL
            val request = Request.Builder().url(url).build()
            val response = OkHttpConnectionFactory.client.newCall(request).execute()
            val campaigns = JsonUtil.decodeFromString<List<JsonElement>>(response.body.string()).orEmpty()
            val now = LocalDateTime.now()
            val mostRecentDonateDateTime = (Prefs.donationResults.maxByOrNull { it.dateTime })?.let {
                LocalDateTime.parse(it.dateTime)
            }

            campaignList.addAll(campaigns.filter {
                val proto = JsonUtil.json.decodeFromJsonElement<CampaignProto>(it)
                proto.version == CAMPAIGN_VERSION
            }.map {
                JsonUtil.json.decodeFromJsonElement<Campaign>(it)
            }.filter {
                it.hasPlatform(CAMPAIGN_PLATFORM) &&
                        it.countries.contains(GeoUtil.geoIPCountry) &&
                        (Prefs.ignoreDateForAnnouncements || (it.startDateTime?.isBefore(now) == true && it.endDateTime?.isAfter(now) == true)) &&
                        (mostRecentDonateDateTime == null || mostRecentDonateDateTime.isBefore(now.minusDays(DAYS_TO_HIDE_AFTER_DONATION)))
            })
        }
        return campaignList
    }

    fun getFormattedCampaignId(campaignId: String): String {
        return "${WikipediaApp.instance.appOrSystemLanguageCode}${GeoUtil.geoIPCountry.orEmpty()}_${campaignId}_${CAMPAIGN_PLATFORM}"
    }

    fun addDonationResult(fromWeb: Boolean = false, amount: Float, currency: String, recurring: Boolean) {
        Prefs.donationResults = Prefs.donationResults.plus(DonationResult(
            LocalDateTime.now().toString(), fromWeb, amount, currency, recurring
        ))
    }

    fun replaceAssetsParams(assets: Campaign.Assets, campaignId: String): Campaign.Assets {
        return Campaign.Assets(
            id = assets.id,
            weight = assets.weight,
            text = if (assets.text.isNullOrEmpty()) null else replaceParams(assets.text, campaignId),
            footer = if (assets.footer.isNullOrEmpty()) null else replaceParams(assets.footer, campaignId),
            actions = assets.actions.map { action ->
                Campaign.Action(
                    replaceParams(action.title, campaignId),
                    if (action.url.isNullOrEmpty()) null else replaceParams(action.url, campaignId)
                )
            }.toTypedArray()
        )
    }

    private fun replaceParams(message: String, campaignId: String): String {
        return message
            .replace("\$platform;", CAMPAIGN_PLATFORM)
            .replace("\$formattedId;", getFormattedCampaignId(campaignId))
            .replace("\$country;", GeoUtil.geoIPCountry.orEmpty())
            .replace("\$language;", WikipediaApp.instance.appOrSystemLanguageCode)
    }

    @Serializable
    class CampaignProto(
        val version: Int = 0
    )
}
