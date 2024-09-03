package org.wikipedia.feed.announcement

import android.location.Location
import androidx.annotation.VisibleForTesting
import org.wikipedia.dataclient.SharedPreferenceCookieManager
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException

/*
This currently supports the "v4" version of the GeoIP cookie.
For some info about the format and contents of the cookie:
https://phabricator.wikimedia.org/diffusion/ECNO/browse/master/resources/subscribing/ext.centralNotice.geoIP.js
 */
object GeoIPCookieUnmarshaller {
    private const val COOKIE_NAME = "GeoIP"

    fun unmarshal(): GeoIPCookie {
        return unmarshal(SharedPreferenceCookieManager.instance.getCookieValueByName(COOKIE_NAME))
    }

    @VisibleForTesting
    fun unmarshal(cookie: String?): GeoIPCookie {
        require(!cookie.isNullOrEmpty()) { "Cookie is empty." }
        val components = cookie.split(":".toRegex()).toTypedArray()
        require(components.size >= Component.entries.size) { "Cookie is malformed." }
        require(components[Component.VERSION.ordinal] == "v4") { "Incorrect cookie version." }

        var location: Location? = null

        if (components[Component.LATITUDE.ordinal].isNotEmpty() &&
            components[Component.LONGITUDE.ordinal].isNotEmpty()) {
            location = Location("")
            try {
                location.latitude = components[Component.LATITUDE.ordinal].toDouble()
                location.longitude = components[Component.LONGITUDE.ordinal].toDouble()
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Location is malformed.")
            }
        }
        return GeoIPCookie(
            components[Component.COUNTRY.ordinal],
            components[Component.REGION.ordinal],
            components[Component.CITY.ordinal],
            location
        )
    }

    private enum class Component {
        COUNTRY, REGION, CITY, LATITUDE, LONGITUDE, VERSION
    }
}
