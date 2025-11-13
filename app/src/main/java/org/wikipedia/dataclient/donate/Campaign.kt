package org.wikipedia.dataclient.donate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.json.LocalDateTimeSerializer
import java.time.LocalDateTime
import kotlin.random.Random

@Suppress("unused")
@Serializable
class Campaign(
    val version: Int,
    private val id: String = "",
    @SerialName("start_time") @Serializable(with = LocalDateTimeSerializer::class) val startDateTime: LocalDateTime? = null,
    @SerialName("end_time") @Serializable(with = LocalDateTimeSerializer::class) val endDateTime: LocalDateTime? = null,
    val platforms: Map<String, PlatformParams> = emptyMap(),
    val countries: List<String> = emptyList(),
    private val assets: Map<String, List<Assets>> = emptyMap()
) {
    fun hasPlatform(platform: String): Boolean {
        return platforms.containsKey(platform)
    }

    fun getIdForLang(lang: String): String {
        val assetsForLang = getAssetsForLang(lang)
        if (assetsForLang == null || assetsForLang.id.isEmpty()) {
            return id
        }
        return id + "_" + assetsForLang.id
    }

    fun getAssetsForLang(lang: String): Assets? {
        val list = assets[lang]
        if (list.isNullOrEmpty()) {
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
        return assets.lastIndex
    }

    @Serializable
    class Assets(
        val id: String = "",
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
