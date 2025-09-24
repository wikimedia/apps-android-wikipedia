package org.wikipedia.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.json.JsonUtil
import org.wikipedia.util.log.L

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
    class RemoteConfigCommonV1

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
}
