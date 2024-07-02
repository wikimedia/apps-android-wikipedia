package org.wikipedia.dataclient.donate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.Request
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.json.JsonUtil

object DonationConfigHelper {

    const val DONATE_WIKI_URL = "https://donate.wikimedia.org/"

    private const val CONFIG_VERSION = 1
    private const val CONFIG_URL = DONATE_WIKI_URL + "wiki/MediaWiki:AppsDonationConfig.json?action=raw"

    suspend fun getConfig(): DonationConfig? {
        val campaignList = mutableListOf<DonationConfig>()

        withContext(Dispatchers.IO) {
            val url = CONFIG_URL
            val request = Request.Builder().url(url).build()
            val response = OkHttpConnectionFactory.client.newCall(request).execute()
            val configs = JsonUtil.decodeFromString<List<JsonElement>>(response.body?.string()).orEmpty()

            campaignList.addAll(configs.filter {
                val proto = JsonUtil.json.decodeFromJsonElement<ConfigProto>(it)
                proto.version == CONFIG_VERSION
            }.map {
                JsonUtil.json.decodeFromJsonElement<DonationConfig>(it)
            })
        }
        return campaignList.firstOrNull()
    }

    @Serializable
    class ConfigProto(
        val version: Int = 0
    )
}
