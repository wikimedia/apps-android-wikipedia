package org.wikipedia.feed.announcement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.feed.announcement.GeoIPCookieUnmarshaller.unmarshal

@RunWith(RobolectricTestRunner::class)
class GeoIPCookieUnmarshallerTest {
    @Test
    fun testGeoIPWithLocation() {
        val cookie = unmarshal("US:California:San Francisco:$LATITUDE:$LONGITUDE:v4")
        assertEquals("US", cookie.country)
        assertEquals("California", cookie.region)
        assertEquals("San Francisco", cookie.city)
        assertNotNull(cookie.location)
        assertEquals(LATITUDE, cookie.location?.latitude)
        assertEquals(LONGITUDE, cookie.location?.longitude)
    }

    @Test
    fun testGeoIPWithoutLocation() {
        val cookie = unmarshal("FR::Paris:::v4")
        assertEquals("FR", cookie.country)
        assertEquals("", cookie.region)
        assertEquals("Paris", cookie.city)
        assertNull(cookie.location)
    }

    @Test
    fun testGeoIPEmpty() {
        val cookie = unmarshal(":::::v4")
        assertEquals("", cookie.country)
        assertEquals("", cookie.region)
        assertEquals("", cookie.city)
        assertNull(cookie.location)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGeoIPWrongVersion() {
        unmarshal("RU::Moscow:1:2:v5")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGeoIPWrongParamCount() {
        unmarshal("CA:Toronto:v4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGeoIPMalformed() {
        unmarshal("foo")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGeoIPWithBadLocation() {
        unmarshal("US:California:San Francisco:foo:bar:v4")
    }

    companion object {
        private const val LATITUDE = 37.33
        private const val LONGITUDE = -121.89
    }
}
