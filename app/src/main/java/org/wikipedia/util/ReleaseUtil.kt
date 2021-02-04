package org.wikipedia.util

import android.content.Context
import android.content.pm.PackageManager
import org.wikipedia.BuildConfig
import org.wikipedia.settings.Prefs

object ReleaseUtil {
    private const val RELEASE_PROD = 0
    private const val RELEASE_BETA = 1
    private const val RELEASE_ALPHA = 2
    private const val RELEASE_DEV = 3
    val isProdRelease: Boolean
        get() = calculateReleaseType() == RELEASE_PROD
    val isPreProdRelease: Boolean
        get() = calculateReleaseType() != RELEASE_PROD
    val isAlphaRelease: Boolean
        get() = calculateReleaseType() == RELEASE_ALPHA
    val isPreBetaRelease: Boolean
        get() = when (calculateReleaseType()) {
            RELEASE_PROD, RELEASE_BETA -> false
            else -> true
        }
    val isDevRelease: Boolean
        get() = calculateReleaseType() == RELEASE_DEV

    @JvmStatic
    fun getChannel(ctx: Context): String {
        var channel = Prefs.getAppChannel()
        if (channel == null) {
            channel = getChannelFromManifest(ctx)
            Prefs.setAppChannel(channel)
        }
        return channel
    }

    private fun calculateReleaseType(): Int {
        if (BuildConfig.APPLICATION_ID.contains("beta")) {
            return RELEASE_BETA
        }
        if (BuildConfig.APPLICATION_ID.contains("alpha")) {
            return RELEASE_ALPHA
        }
        return if (BuildConfig.APPLICATION_ID.contains("dev")) {
            RELEASE_DEV
        } else {
            RELEASE_PROD
        }
    }

    private fun getChannelFromManifest(ctx: Context): String {
        return try {
            val info = ctx.packageManager
                    .getApplicationInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA)
            val channel = info.metaData.getString(Prefs.getAppChannelKey())
            channel ?: ""
        } catch (t: Throwable) {
            ""
        }
    }
}
