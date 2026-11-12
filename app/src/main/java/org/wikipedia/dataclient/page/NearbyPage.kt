package org.wikipedia.dataclient.page

import android.location.Location
import org.wikipedia.page.PageTitle

class NearbyPage(
    val pageId: Int,
    val pageTitle: PageTitle,
    val latitude: Double,
    val longitude: Double
) {
    val location get() = Location("").apply {
        latitude = this@NearbyPage.latitude
        longitude = this@NearbyPage.longitude
    }
}
