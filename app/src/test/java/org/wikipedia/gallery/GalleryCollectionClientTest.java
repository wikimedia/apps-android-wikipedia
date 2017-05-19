package org.wikipedia.gallery;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("checkstyle:magicnumber")
public class GalleryCollectionClientTest extends MockWebServerTest {
    private static final WikiSite TESTWIKI = WikiSite.forLanguageCode("test");
    private static final PageTitle TITLE = new PageTitle("William_Henry_Bury", TESTWIKI);

    @NonNull private final GalleryCollectionClient subject = new GalleryCollectionClient();

    @Test public void testRequestSuccessNoThumbs() throws Throwable {
        enqueueFromFile("gallery_collection_no_thumbs.json");
        Map<String, ImageInfo> result = request(false);
        assertThat(result.size(), is(4));
        assertNoUrls(result);
    }

    @Test public void testRequestSuccessWithThumbs() throws Throwable {
        enqueueFromFile("gallery_collection_with_thumbs.json");
        Map<String, ImageInfo> result = request(true);
        assertThat(result.size(), is(4));
        assertHasUrls(result);
    }

    @Test public void testRequestSuccessWithContinuation() throws Throwable {
        enqueueFromFile("gallery_collection_with_continuation_1.json");
        enqueueFromFile("gallery_collection_with_continuation_2.json");
        Map<String, ImageInfo> result = request(true);
        assertThat(result.size(), is(2));
    }

    @Test(expected = MwException.class) public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");
        request(false);
    }

    @Test(expected = HttpStatusException.class) public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        request(false);
    }

    @Test(expected = MalformedJsonException.class) public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");
        request(false);
    }

    private void assertNoUrls(@NonNull Map<String, ImageInfo> subject) {
        for (Map.Entry<String, ImageInfo> entry : subject.entrySet()) {
            ImageInfo info = entry.getValue();
            assertThat(info.getOriginalUrl(), is((String) null));
            assertThat(info.getDescriptionUrl(), is((String) null));
            assertThat(info.getDescriptionShortUrl(), is((String) null));
        }
    }

    private void assertHasUrls(@NonNull Map<String, ImageInfo> subject) {
        for (Map.Entry<String, ImageInfo> entry : subject.entrySet()) {
            ImageInfo info = entry.getValue();
            assertThat(isNotBlank(info.getOriginalUrl()), is(true));
            assertThat(isNotBlank(info.getDescriptionUrl()), is(true));
            assertThat(isNotBlank(info.getDescriptionShortUrl()), is(true));
        }
    }

    private Map<String, ImageInfo> request(boolean getThumbs) throws Throwable {
        return subject.request(service(GalleryCollectionClient.Service.class), TITLE, getThumbs);
    }
}
