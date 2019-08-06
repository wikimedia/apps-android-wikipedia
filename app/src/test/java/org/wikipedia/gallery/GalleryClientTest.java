package org.wikipedia.gallery;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.test.MockRetrofitTest;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

public class GalleryClientTest extends MockRetrofitTest {
    private static final String RAW_JSON_FILE = "gallery.json";

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAllSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<MediaList> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> gallery.getItems("image").size() == 1
                        && gallery.getItems("video").size() == 1);
    }

    @Test
    public void testRequestImageSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<MediaList> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<MediaListItem> result = gallery.getItems("image");
                    return result.size() == 1
                            && result.get(0).getType().equals("image")
                            && result.get(0).getTitle().equals("File:BarackObamaportrait.jpg")
                            && result.get(0).showInGallery();
                });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestVideoSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<MediaList> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<MediaListItem> result = gallery.getItems("video");
                    return result.get(0).getType().equals("video")
                            && result.get(0).getTitle().equals("File:20090124_WeeklyAddress.ogv")
                            && result.get(0).showInGallery();
                });
    }

    @Test public void testRequestResponseFailure() {
        enqueue404();
        TestObserver<MediaList> observer = new TestObserver<>();
        getObservable().subscribe(observer);
        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<MediaList> observer = new TestObserver<>();
        getObservable().subscribe(observer);
        observer.assertError(MalformedJsonException.class);
    }

    private Observable<MediaList> getObservable() {
        return getRestService().getMediaList("foo");
    }
}
