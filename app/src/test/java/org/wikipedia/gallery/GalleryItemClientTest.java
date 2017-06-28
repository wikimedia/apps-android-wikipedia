package org.wikipedia.gallery;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.gallery.GalleryItemClient.Callback;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GalleryItemClientTest extends MockWebServerTest {

    private static final WikiSite WIKISITE_EN = WikiSite.forLanguageCode("en");
    private static final PageTitle PAGE_TITLE_IMAGE = new PageTitle("File", "Kozanji_Kyoto_Kyoto11s5s4592", WIKISITE_EN);
    private static final PageTitle PAGE_TITLE_VIDEO = new PageTitle("File", "Wood cleaving - 2016.webm", WIKISITE_EN);

    @NonNull private final GalleryItemClient subject = new GalleryItemClient();

    @Test public void testRequestSuccessForImage() throws Throwable {
        enqueueFromFile("gallery_item_image.json");

        Callback cb = mock(Callback.class);
        Call<MwQueryResponse> call = request(cb, false);
        server().takeRequest();
        ArgumentCaptor<GalleryItem> captor = ArgumentCaptor.forClass(GalleryItem.class);

        //noinspection unchecked
        verify(cb).success(eq(call), captor.capture());
        //noinspection unchecked
        GalleryItem galleryItem = captor.getValue();

        assertThat(galleryItem != null, is(true));
        assertThat(String.valueOf(galleryItem.getHeight()), is("1489"));
        assertThat(String.valueOf(galleryItem.getWidth()), is("2125"));
        assertThat(galleryItem.getThumbUrl(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Kinkaku3402CBcropped.jpg/1280px-Kinkaku3402CBcropped.jpg"));
        assertThat(galleryItem.getMimeType(), is("image/jpeg"));
        assertThat(galleryItem.getUrl(), is("https://upload.wikimedia.org/wikipedia/commons/c/c9/Kinkaku3402CBcropped.jpg"));
    }

    @Test @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccessForVideo() throws Throwable {
        enqueueFromFile("gallery_item_video.json");

        Callback cb = mock(Callback.class);
        Call<MwQueryResponse> call = request(cb, true);
        server().takeRequest();
        ArgumentCaptor<GalleryItem> captor = ArgumentCaptor.forClass(GalleryItem.class);

        //noinspection unchecked
        verify(cb).success(eq(call), captor.capture());
        //noinspection unchecked
        GalleryItem galleryItem = captor.getValue();

        assertThat(galleryItem != null, is(true));
        assertThat(String.valueOf(galleryItem.getHeight()), is("720"));
        assertThat(String.valueOf(galleryItem.getWidth()), is("400"));
        assertThat(galleryItem.getThumbUrl(), is("https://upload.wikimedia.org/wikipedia/commons/thumb/e/eb/Wood_cleaving_-_2016.webm/400px--Wood_cleaving_-_2016.webm.jpg"));
        assertThat(galleryItem.getMimeType(), is("video/webm"));
        assertThat(galleryItem.getUrl(), is("https://upload.wikimedia.org/wikipedia/commons/e/eb/Wood_cleaving_-_2016.webm"));
        assertThat(galleryItem.getDerivatives().size(), is(11));
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        GalleryItemClient.Callback cb = mock(GalleryItemClient.Callback.class);
        Call<MwQueryResponse> call = request(cb, false);
        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        GalleryItemClient.Callback cb = mock(GalleryItemClient.Callback.class);
        Call<MwQueryResponse> call = request(cb, false);
        server().takeRequest();
        assertCallbackFailure(call, cb, HttpStatusException.class);
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        GalleryItemClient.Callback cb = mock(GalleryItemClient.Callback.class);
        Call<MwQueryResponse> call = request(cb, false);

        server().takeRequest();
        assertCallbackFailure(call, cb, MwException.class);
    }

    private void assertCallbackFailure(@NonNull Call<MwQueryResponse> call,
                                       @NonNull GalleryItemClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(GalleryItem.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<MwQueryResponse> request(@NonNull Callback cb, boolean isVideo) {
        return subject.request(service(GalleryItemClient.Service.class), isVideo ? PAGE_TITLE_VIDEO
                : PAGE_TITLE_IMAGE, cb, isVideo);
    }
}
