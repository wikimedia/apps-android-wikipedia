package org.wikipedia.dataclient.okhttp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;

import okhttp3.HttpUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricTestRunner.class) public class HttpUrlUtilTest {
    @Test public void testIsRestBaseProd() {
        HttpUrl url = HttpUrl.parse("https://test.wikipedia.org/api/rest_v1/");
        assertThat(HttpUrlUtil.isRestBase(url), is(true));
    }

    @Test public void testIsRestBaseLabs() {
        HttpUrl url = HttpUrl.parse("http://appservice.wmflabs.org/test.wikipedia.org/v1/");
        assertThat(HttpUrlUtil.isRestBase(url), is(true));
    }

    @Test public void testIsRestBaseDev() {
        HttpUrl url = HttpUrl.parse("http://host:6927/192.168.1.11:8080/v1/");
        assertThat(HttpUrlUtil.isRestBase(url), is(true));
    }

    @Test public void testIsRestBaseMediaWikiTest() {
        HttpUrl url = HttpUrl.parse(WikiSite.forLanguageCode("test").url());
        assertThat(HttpUrlUtil.isRestBase(url), is(false));
    }

    @Test public void testIsRestBaseMediaWikiDev() {
        HttpUrl url = HttpUrl.parse("http://192.168.1.11:8080/");
        assertThat(HttpUrlUtil.isRestBase(url), is(false));
    }

    @Test public void testIsMobileViewTest() {
        HttpUrl url = HttpUrl.parse(WikiSite.forLanguageCode("test").url())
                .newBuilder()
                .addQueryParameter("action", "mobileview")
                .build();
        assertThat(HttpUrlUtil.isMobileView(url), is(true));
    }

    @Test public void testIsMobileViewDev() {
        HttpUrl url = HttpUrl.parse("http://localhost:8080/?action=mobileview");
        assertThat(HttpUrlUtil.isMobileView(url), is(true));
    }

    @Test public void testIsMobileViewRestBase() {
        HttpUrl url = HttpUrl.parse("https://en.wikipedia.org/api/rest_v1/");
        assertThat(HttpUrlUtil.isMobileView(url), is(false));
    }
}
