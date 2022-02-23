package org.wikipedia.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.net.Uri
import org.wikipedia.R
import org.wikipedia.feed.announcement.GeoIPCookieUnmarshaller

object GeoUtil {
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

    val geoIPCountry: String?
        get() = try {
            GeoIPCookieUnmarshaller.unmarshal().country()
        } catch (e: IllegalArgumentException) {
            // For our purposes, don't care about malformations in the GeoIP cookie for now.
            null
        }
}
