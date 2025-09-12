package org.wikipedia.settings

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
    }
}
