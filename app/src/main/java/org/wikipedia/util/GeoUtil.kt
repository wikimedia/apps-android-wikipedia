package org.wikipedia.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.net.Uri
import org.wikipedia.R
import org.wikipedia.feed.announcement.GeoIPCookieUnmarshaller
import org.wikipedia.history.db.HistoryEntryWithImage
import org.wikipedia.settings.Prefs
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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








    private const val EARTH_RADIUS_KM = 6371.0

    data class Cluster(
        val id: Int,
        val locations: MutableList<HistoryEntryWithImage> = mutableListOf(),
        val centroid: Location? = null
    )

    class LocationClusterer {
        fun clusterLocations(
            locations: List<HistoryEntryWithImage>,
            epsilonKm: Double = 0.5, // 500 meters default
            minPoints: Int = 3
        ): List<Cluster> {
            val clusters = mutableListOf<Cluster>()
            val visited = mutableSetOf<HistoryEntryWithImage>()
            val noise = mutableSetOf<HistoryEntryWithImage>()
            var clusterId = 0

            for (location in locations) {
                if (location in visited) continue
                visited.add(location)

                val neighbors = getNeighbors(location, locations, epsilonKm)

                if (neighbors.size < minPoints) {
                    noise.add(location)
                } else {
                    val cluster = Cluster(id = clusterId++)
                    expandCluster(location, neighbors, cluster, visited, locations, epsilonKm, minPoints)

                    // Calculate centroid for the cluster
                    if (cluster.locations.isNotEmpty()) {
                        cluster.copy(centroid = calculateCentroid(cluster.locations))
                        clusters.add(cluster)
                    }
                }
            }

            // Add centroids to clusters
            return clusters.map { cluster ->
                cluster.copy(centroid = calculateCentroid(cluster.locations))
            }
        }

        private fun expandCluster(
            location: HistoryEntryWithImage,
            neighbors: MutableList<HistoryEntryWithImage>,
            cluster: Cluster,
            visited: MutableSet<HistoryEntryWithImage>,
            allLocations: List<HistoryEntryWithImage>,
            epsilon: Double,
            minPoints: Int
        ) {
            cluster.locations.add(location)

            var i = 0
            while (i < neighbors.size) {
                val neighbor = neighbors[i]

                if (neighbor !in visited) {
                    visited.add(neighbor)
                    val neighborNeighbors = getNeighbors(neighbor, allLocations, epsilon)

                    if (neighborNeighbors.size >= minPoints) {
                        neighbors.addAll(neighborNeighbors.filter { it !in neighbors })
                    }
                }

                if (cluster.locations.none { it == neighbor }) {
                    cluster.locations.add(neighbor)
                }
                i++
            }
        }

        private fun getNeighbors(
            location: HistoryEntryWithImage,
            allLocations: List<HistoryEntryWithImage>,
            epsilonKm: Double
        ): MutableList<HistoryEntryWithImage> {
            return allLocations.filter { other ->
                other != location && haversineDistance(location, other) <= epsilonKm
            }.toMutableList()
        }

        private fun calculateCentroid(locations: List<HistoryEntryWithImage>): Location {
            if (locations.isEmpty())
                throw IllegalArgumentException("Cannot calculate centroid of empty list")

            val avgLat = locations.map { it.geoLat ?: 0.0 }.average()
            val avgLon = locations.map { it.geoLon ?: 0.0 }.average()

            return Location("centroid").also {
                it.latitude = avgLat
                it.longitude = avgLon
            }
        }

        /**
         * Calculate distance between two locations using Haversine formula
         * @return Distance in kilometers
         */
        private fun haversineDistance(loc1: HistoryEntryWithImage, loc2: HistoryEntryWithImage): Double {
            val lat1 = loc1.geoLat ?: 0.0
            val lon1 = loc1.geoLon ?: 0.0
            val lat2 = loc2.geoLat ?: 0.0
            val lon2 = loc2.geoLon ?: 0.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_KM * c
        }
    }
}
