package org.wikipedia.dataclient.donate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import java.time.LocalDateTime

object CampaignCollection {

    private const val CAMPAIGN_VERSION = 1

    private const val CAMPAIGNS_URL = "https://donate.wikimedia.org/wiki/MediaWiki:AppsCampaignConfig.json?action=raw"
    private const val CAMPAIGNS_URL_DEBUG = "https://donate.wikimedia.org/wiki/MediaWiki:AppsCampaignConfigStaging.json?action=raw"

    suspend fun getActiveCampaigns(): List<Campaign> {
        val campaignList = mutableListOf<Campaign>()

        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(CAMPAIGNS_URL_DEBUG).build()
            val response = OkHttpConnectionFactory.client.newCall(request).execute()
            val json = response.body!!.string()

            Thread.sleep(5000)

            val campaigns = JsonUtil.decodeFromString<List<JsonElement>>(json).orEmpty()

            campaignList.addAll(campaigns.filter {
                val proto = JsonUtil.json.decodeFromJsonElement<CampaignProto>(it)
                proto.version == CAMPAIGN_VERSION
            }.map {
                JsonUtil.json.decodeFromJsonElement<Campaign>(it)
            }.filter {
                it.hasPlatform("Android") &&
                        it.countries.contains(GeoUtil.geoIPCountry) &&
                        (Prefs.ignoreDateForAnnouncements || (it.startDateTime?.isBefore(LocalDateTime.now()) == true && it.endDateTime?.isAfter(LocalDateTime.now()) == true))
            })
        }
        return campaignList
    }

    @Serializable
    class CampaignProto(
        val version: Int = 0
    )
}
