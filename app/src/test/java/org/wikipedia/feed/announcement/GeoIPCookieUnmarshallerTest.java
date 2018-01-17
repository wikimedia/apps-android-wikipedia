package org.wikipedia.feed.announcement;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@RunWith(RobolectricTestRunner.class)
public class GeoIPCookieUnmarshallerTest {

    private static final double LATITUDE = 37.33;
    private static final double LONGITUDE = -121.89;

    @Test public void testGeoIPWithLocation() {
        GeoIPCookie cookie = GeoIPCookieUnmarshaller.unmarshal("US:California:San Francisco:" + LATITUDE + ":" + LONGITUDE + ":v4");
        assertThat(cookie.country(), is("US"));
        assertThat(cookie.region(), is("California"));
        assertThat(cookie.city(), is("San Francisco"));
        assertThat(cookie.location(), is(notNullValue()));
        assertThat(cookie.location().getLatitude(), is(LATITUDE));
        assertThat(cookie.location().getLongitude(), is(LONGITUDE));
    }

    @Test public void testGeoIPWithoutLocation() {
        GeoIPCookie cookie = GeoIPCookieUnmarshaller.unmarshal("FR::Paris:::v4");
        assertThat(cookie.country(), is("FR"));
        assertThat(cookie.region(), is(""));
        assertThat(cookie.city(), is("Paris"));
        assertThat(cookie.location(), is(nullValue()));
    }

    @Test public void testGeoIPEmpty() {
        GeoIPCookie cookie = GeoIPCookieUnmarshaller.unmarshal(":::::v4");
        assertThat(cookie.country(), is(""));
        assertThat(cookie.region(), is(""));
        assertThat(cookie.city(), is(""));
        assertThat(cookie.location(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIPWrongVersion() {
        GeoIPCookieUnmarshaller.unmarshal("RU::Moscow:1:2:v5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIPWrongParamCount() {
        GeoIPCookieUnmarshaller.unmarshal("CA:Toronto:v4");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIPMalformed() {
        GeoIPCookieUnmarshaller.unmarshal("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoIPWithBadLocation() {
        GeoIPCookieUnmarshaller.unmarshal("US:California:San Francisco:foo:bar:v4");
    }
}
