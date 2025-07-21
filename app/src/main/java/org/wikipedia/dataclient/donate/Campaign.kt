package org.wikipedia.dataclient.donate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DateUtil
import kotlin.random.Random

@Suppress("unused")
@Serializable
class Campaign(
    val version: Int,
    private val id: String = "",
    @SerialName("start_time") private val startTime: String? = null,
    @SerialName("end_time") private val endTime: String? = null,
    val platforms: Map<String, PlatformParams> = emptyMap(),
    val countries: List<String> = emptyList(),
    private val assets: Map<String, List<Assets>> = emptyMap()
) {
    val startDateTime get() = startTime?.let { DateUtil.iso8601LocalDateTimeParse(it) }

    val endDateTime get() = endTime?.let { DateUtil.iso8601LocalDateTimeParse(it) }

    fun hasPlatform(platform: String): Boolean {
        return platforms.containsKey(platform)
    }

    fun getIdForLang(lang: String): String {
        val list = assets[lang].orEmpty()
        if (list.size <= 1) {
            return id
        }
        val testGroup = abTestGroup(list)
        return id + "_" + testGroup
    }

    fun getAssetsForLang(lang: String): Assets? {
        val list = assets[lang].orEmpty()
        if (list.isEmpty()) {
            return null
        } else if (list.size == 1) {
            return list[0]
        }
        val testGroup = abTestGroup(list)
        return list[testGroup]
    }

    private fun abTestGroup(assets: List<Assets>): Int {
        if (assets.size <= 1) {
            return 0
        }
        val random = Random(WikipediaApp.instance.appInstallID.hashCode() + id.hashCode())
        val f = random.nextFloat()
        var sum = 0f
        for (i in assets.indices) {
            sum += assets[i].weight
            if (f <= sum) {
                return i
            }
        }
        return assets.size - 1
    }

    @Serializable
    class Assets(
        val weight: Float = 1f,
        val text: String? = "",
        val footer: String? = "",
        val actions: Array<Action> = emptyArray()
    )

    @Serializable
    class Action(
        val title: String = "",
        val url: String? = null
    )

    @Serializable
    class PlatformParams
}
