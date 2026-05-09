package org.wikipedia.places

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.NearbyPage
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class SortNearbyPagesTest {

    @Test
    fun popularitySort_orderedByPageViewsDescending() {
        val pages = listOf(
            page(id = 1, lat = 0.0, lon = 0.0, views = 100),
            page(id = 2, lat = 0.0, lon = 0.0, views = 9000),
            page(id = 3, lat = 0.0, lon = 0.0, views = 50)
        )

        val sorted = sortNearbyPages(pages, SortMode.POPULARITY, lastKnownLocation = null)

        assertEquals(listOf(2, 1, 3), sorted.map { it.pageId })
    }

    @Test
    fun distanceSort_orderedByProximityToLastKnownLocation() {
        val origin = location(0.0, 0.0)
        val pages = listOf(
            page(id = 1, lat = 1.0, lon = 0.0, views = 0),
            page(id = 2, lat = 0.1, lon = 0.0, views = 0),
            page(id = 3, lat = 0.5, lon = 0.0, views = 0)
        )

        val sorted = sortNearbyPages(pages, SortMode.DISTANCE, lastKnownLocation = origin)

        assertEquals(listOf(2, 3, 1), sorted.map { it.pageId })
    }

    @Test
    fun distanceSort_withoutLocation_isStableNoOp() {
        val pages = listOf(
            page(id = 1, lat = 5.0, lon = 5.0, views = 100),
            page(id = 2, lat = 0.0, lon = 0.0, views = 9000)
        )

        val sorted = sortNearbyPages(pages, SortMode.DISTANCE, lastKnownLocation = null)

        assertEquals(listOf(1, 2), sorted.map { it.pageId })
    }

    @Test
    fun popularitySort_zeroViewsSortLast() {
        val pages = listOf(
            page(id = 1, lat = 0.0, lon = 0.0, views = 0),
            page(id = 2, lat = 0.0, lon = 0.0, views = 1),
            page(id = 3, lat = 0.0, lon = 0.0, views = 0)
        )

        val sorted = sortNearbyPages(pages, SortMode.POPULARITY, lastKnownLocation = null)

        assertEquals(2, sorted.first().pageId)
    }

    private fun page(id: Int, lat: Double, lon: Double, views: Long) = NearbyPage(
        pageId = id,
        pageTitle = PageTitle("Page $id", WIKI),
        latitude = lat,
        longitude = lon,
        pageViews = views
    )

    private fun location(lat: Double, lon: Double) = Location("test").apply {
        latitude = lat
        longitude = lon
    }

    companion object {
        private val WIKI = WikiSite.forLanguageCode("en")
    }
}
