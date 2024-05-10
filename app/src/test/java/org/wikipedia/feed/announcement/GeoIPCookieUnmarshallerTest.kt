package org.wikipedia.feed.announcement

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.feed.announcement.GeoIPCookieUnmarshaller.unmarshal

@RunWith(RobolectricTestRunner::class)
class GeoIPCookieUnmarshallerTest {
    @Test
    fun testGeoIPWithLocation() {
        val cookie = unmarshal("US:California:San Francisco:$LATITUDE:$LONGITUDE:v4")
        MatcherAssert.assertThat(cookie.country(), Matchers.`is`("US"))
        MatcherAssert.assertThat(cookie.region(), Matchers.`is`("California"))
        MatcherAssert.assertThat(cookie.city(), Matchers.`is`("San Francisco"))
        MatcherAssert.assertThat(cookie.location(), Matchers.`is`(Matchers.notNullValue()))
        MatcherAssert.assertThat(cookie.location()?.latitude, Matchers.`is`(LATITUDE))
        MatcherAssert.assertThat(cookie.location()?.longitude, Matchers.`is`(LONGITUDE))
    }

    @Test
    fun testGeoIPWithoutLocation() {
        val cookie = unmarshal("FR::Paris:::v4")
        MatcherAssert.assertThat(cookie.country(), Matchers.`is`("FR"))
        MatcherAssert.assertThat(cookie.region(), Matchers.`is`(""))
        MatcherAssert.assertThat(cookie.city(), Matchers.`is`("Paris"))
        MatcherAssert.assertThat(cookie.location(), Matchers.`is`(Matchers.nullValue()))
    }

    @Test
    fun testGeoIPEmpty() {
        val cookie = unmarshal(":::::v4")
        MatcherAssert.assertThat(cookie.country(), Matchers.`is`(""))
        MatcherAssert.assertThat(cookie.region(), Matchers.`is`(""))
        MatcherAssert.assertThat(cookie.city(), Matchers.`is`(""))
        MatcherAssert.assertThat(cookie.location(), Matchers.`is`(Matchers.nullValue()))
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
