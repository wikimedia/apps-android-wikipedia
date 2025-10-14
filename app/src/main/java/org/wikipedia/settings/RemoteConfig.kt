package org.wikipedia.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.JsonUtil
import org.wikipedia.json.LocalDateTimeSerializer
import org.wikipedia.util.log.L
import java.time.LocalDateTime

object RemoteConfig {
    private var curConfig: RemoteConfigImpl? = null

    val config: RemoteConfigImpl
        get() {
            if (curConfig == null) {
                curConfig = try {
                    JsonUtil.decodeFromString<RemoteConfigImpl>(Prefs.remoteConfigJson)
                } catch (e: Exception) {
                    L.e(e)
                    RemoteConfigImpl()
                }
            }
            return curConfig!!
        }

    fun updateConfig(config: RemoteConfigImpl) {
        Prefs.remoteConfigJson = JsonUtil.encodeToString(config).orEmpty()
        curConfig = null
    }

    @Suppress("unused")
    @Serializable
    class RemoteConfigImpl {
        val commonv1: RemoteConfigCommonV1? = null
        val androidv1: RemoteConfigAndroidV1? = null

        val disableReadingListSync
            get() = androidv1?.disableReadingListSync == true
    }

    @Suppress("unused")
    @Serializable
    class RemoteConfigCommonV1 {
        val yir: RemoteConfigYearInReview? = null
    }

    @Suppress("unused")
    @Serializable
    class RemoteConfigAndroidV1 {
        val disableReadingListSync = false
        val hCaptcha: RemoteConfigHCaptcha? = null
    }

    @Suppress("unused")
    @Serializable
    class RemoteConfigHCaptcha(
        val baseURL: String = "",
        val jsSrc: String = "",
        val endpoint: String = "",
        @SerialName("assethost") val assetHost: String = "",
        @SerialName("imghost") val imgHost: String = "",
        @SerialName("reportapi") val reportApi: String = "",
        val sentry: Boolean = false,
        val siteKey: String = ""
    )

    @Suppress("unused")
    @Serializable
    class RemoteConfigYearInReview {
        val year: Int = 0
        @Serializable(with = LocalDateTimeSerializer::class) val activeStartDate: LocalDateTime = LocalDateTime.now()
        @Serializable(with = LocalDateTimeSerializer::class) val activeEndDate: LocalDateTime = LocalDateTime.now()
        @Serializable(with = LocalDateTimeSerializer::class) val dataStartDate: LocalDateTime = LocalDateTime.now()
        @Serializable(with = LocalDateTimeSerializer::class) val dataEndDate: LocalDateTime = LocalDateTime.now()
        val languages: Int = 0
        val articles: Long = 0
        val savedArticlesApps: Long = 0
        val viewsApps: Long = 0
        val editsApps: Long = 0
        val editsPerMinute: Int = 0
        val averageArticlesReadPerYear: Int = 0
        val edits: Long = 0
        val editsEN: Long = 0
        val bytesAddedEN: Long = 0
        val hoursReadEN: Long = 0
        val yearsReadEN: Int = 0
        val topReadEN: List<String> = emptyList()
        val topReadPercentages: List<TopReadPercentage> = emptyList()
        val hideCountryCodes: List<String> = emptyList()
        val hideDonateCountryCodes: List<String> = emptyList()
    }

    @Serializable
    class TopReadPercentage(
        val identifier: String = "",
        val min: Int? = null,
        val max: Int? = null
    )
}
