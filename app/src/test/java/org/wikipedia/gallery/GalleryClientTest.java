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

        TestObserver<Gallery> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<GalleryItem> result = gallery.getAllItems();
                    return result != null
                            && result.get(0).getType().equals("image")
                            && result.get(2).getType().equals("audio")
                            && result.get(4).getType().equals("video");
                });
    }

    @Test
    public void testRequestImageSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<Gallery> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<GalleryItem> result = gallery.getItems("image");
                    return result.size() == 2
                            && result.get(0).getType().equals("image")
                            && result.get(0).getTitles().getCanonical().equals("File:Flag_of_the_United_States.svg")
                            && result.get(0).getThumbnail().getSource().equals("http://upload.wikimedia.org/wikipedia/en/thumb/a/a4/Flag_of_the_United_States.svg/320px-Flag_of_the_United_States.svg.png")
                            && result.get(0).getThumbnailUrl().equals("http://upload.wikimedia.org/wikipedia/en/thumb/a/a4/Flag_of_the_United_States.svg/320px-Flag_of_the_United_States.svg.png")
                            && result.get(0).getPreferredSizedImageUrl().equals("http://upload.wikimedia.org/wikipedia/en/thumb/a/a4/Flag_of_the_United_States.svg/1280px-Flag_of_the_United_States.svg.png");
                });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestVideoSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<Gallery> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<GalleryItem> result = gallery.getItems("video");
                    return result.get(0).getSources().size() == 6
                            && result.get(0).getType().equals("video")
                            && result.get(0).getTitles().getCanonical().equals("File:Curiosity's_Seven_Minutes_of_Terror.ogv")
                            && result.get(0).getFilePage().equals("https://commons.wikimedia.org/wiki/File:Curiosity%27s_Seven_Minutes_of_Terror.ogv")
                            && result.get(0).getOriginalVideoSource().getOriginalUrl().equals("https://upload.wikimedia.org/wikipedia/commons/transcoded/9/96/Curiosity%27s_Seven_Minutes_of_Terror.ogv/Curiosity%27s_Seven_Minutes_of_Terror.ogv.720p.webm");
                });
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAudioSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        TestObserver<Gallery> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(gallery -> {
                    List<GalleryItem> result = gallery.getItems("audio");
                    return result.size() == 2
                            && result.get(0).getType().equals("audio")
                            && result.get(1).getTitles().getCanonical().equals("File:March,_Colonel_John_R._Bourgeois,_Director_·_John_Philip_Sousa_·_United_States_Marine_Band.ogg")
                            && result.get(1).getDuration() == 226.51766666667
                            && result.get(0).getAudioType().equals("generic");
                });
    }

    @Test public void testRequestResponseFailure() {
        enqueue404();
        TestObserver<Gallery> observer = new TestObserver<>();
        getObservable().subscribe(observer);
        observer.assertError(Exception.class);
    }

    @Test public void testRequestResponseMalformed() {
        enqueueMalformed();
        TestObserver<Gallery> observer = new TestObserver<>();
        getObservable().subscribe(observer);
        observer.assertError(MalformedJsonException.class);
    }

    private Observable<Gallery> getObservable() {
        return getRestService().getMedia("foo");
    }
}
