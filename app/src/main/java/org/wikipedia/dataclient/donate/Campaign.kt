package org.wikipedia.dataclient.donate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.util.DateUtil

@Suppress("unused")
@Serializable
class Campaign(
    val version: Int,
    val id: String = "",
    @SerialName("start_time") private val startTime: String? = null,
    @SerialName("end_time") private val endTime: String? = null,
    val platforms: Map<String, PlatformParams> = emptyMap(),
    val countries: List<String> = emptyList(),
    val assets: Map<String, Assets> = emptyMap()
) {
    val startDateTime get() = startTime?.let { DateUtil.iso8601LocalDateTimeParse(it) }

    val endDateTime get() = endTime?.let { DateUtil.iso8601LocalDateTimeParse(it) }

    fun hasPlatform(platform: String): Boolean {
        return platforms.containsKey(platform)
    }

    fun getAssetsForLang(lang: String): Assets? {
        return assets[lang]
    }

    @Serializable
    class Assets(
        val text: String? = "",
        val footer: String? = "",
        val actions: Array<Action> = emptyArray()
    )

    @Serializable
    class Action(
        val title: String = "",
        @SerialName("url") val rawUrl: String? = null
    ) {
        val url get() = rawUrl?.replace("\$platform;", "Android")
    }

    @Serializable
    class PlatformParams
}
