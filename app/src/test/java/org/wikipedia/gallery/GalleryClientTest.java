package org.wikipedia.gallery;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;

public class GalleryClientTest extends MockRetrofitTest {
    private static final String RAW_JSON_FILE = "gallery.json";

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAllSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(gallery -> gallery.getItems("image").size() == 1
                        && gallery.getItems("video").size() == 1);
    }

    @Test
    public void testRequestImageSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<MediaListItem> result = gallery.getItems("image");
                    return result.size() == 1
                            && result.get(0).getType().equals("image")
                            && result.get(0).getTitle().equals("File:BarackObamaportrait.jpg")
                            && result.get(0).isShowInGallery();
                });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestVideoSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<MediaListItem> result = gallery.getItems("video");
                    return result.get(0).getType().equals("video")
                            && result.get(0).getTitle().equals("File:20090124_WeeklyAddress.ogv")
                            && result.get(0).isShowInGallery();
                });
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    private Observable<MediaList> getObservable() {
        return getRestService().getMediaList("foo", 0);
    }
}
