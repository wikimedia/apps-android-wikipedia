package org.wikipedia.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.net.Uri
import org.wikipedia.R
import org.wikipedia.feed.announcement.GeoIPCookieUnmarshaller
import org.wikipedia.settings.Prefs
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object GeoUtil {
    @Suppress("UnsafeImplicitIntentLaunch")
    fun sendGeoIntent(activity: Activity,
                      location: Location,
                      placeName: String?) {
        // Using geo:latitude,longitude doesn't give a point on the map, hence using query
        var geoStr = "geo:0,0?q=${location.latitude},${location.longitude}"
        if (!placeName.isNullOrEmpty()) {
            geoStr += "(${Uri.encode(placeName)})"
        }
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geoStr)))
        } catch (e: ActivityNotFoundException) {
            FeedbackUtil.showMessage(activity, R.string.error_no_maps_app)
        }
    }

    val geoIPCountry
        get() = try {
            if (!Prefs.geoIPCountryOverride.isNullOrEmpty()) {
                Prefs.geoIPCountryOverride
            } else {
                GeoIPCookieUnmarshaller.unmarshal().country
            }
        } catch (e: IllegalArgumentException) {
            // For our purposes, don't care about malformations in the GeoIP cookie for now.
            null
        }

    fun getDistanceWithUnit(startLocation: Location, endLocation: Location, locale: Locale): String {
        val countriesUsingMiles = listOf("US", "GB", "LR", "MM")
        val milesInKilometers = 0.62137119
        val distance = startLocation.distanceTo(endLocation) / 1000.0 // in Kilometers
        val formatter = DecimalFormat("#.##")
        return if (countriesUsingMiles.contains(locale.country)) {
            "${formatter.format(distance * milesInKilometers)} mi"
        } else {
            "${formatter.format(distance)} km"
        }
    }

    fun isSamePlace(startLat: Double, endLat: Double, startLon: Double, endLon: Double): Boolean {
        val tolerance = 0.0000001
        return abs(startLat - endLat) < tolerance && abs(startLon - endLon) < tolerance
    }

    fun currencyFormat(locale: Locale): NumberFormat {
        return NumberFormat.getCurrencyInstance(Locale.Builder().setLocale(locale).setRegion(geoIPCountry.orEmpty()).build())
    }
}
