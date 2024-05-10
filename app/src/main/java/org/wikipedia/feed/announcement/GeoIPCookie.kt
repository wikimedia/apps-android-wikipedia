package org.wikipedia.feed.announcement

import android.location.Location

class GeoIPCookie(
    private val country: String,
    private val region: String,
    private val city: String,
    private val location: Location?
) {
    fun country(): String {
        return country
    }

    fun region(): String {
        return region
    }

    fun city(): String {
        return city
    }

    fun location(): Location? {
        return location
    }
}
