package org.wikipedia.gallery;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.test.MockRetrofitTest;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GalleryClientTest extends MockRetrofitTest {
    private static final String RAW_JSON_FILE = "gallery.json";

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAllSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        getRestService().getMedia("foo").subscribe(gallery -> {
            List<GalleryItem> result = gallery.getAllItems();

            assertThat(result != null, is(true));
            assertThat(result.size(), is(5));
            assertThat(result.get(0).getType(), is("image"));
            assertThat(result.get(2).getType(), is("audio"));
            assertThat(result.get(4).getType(), is("video"));
        }, throwable -> assertTrue(false));
    }

    @Test
    public void testRequestImageSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        getRestService().getMedia("foo").subscribe(gallery -> {
            List<GalleryItem> result = gallery.getItems("image");

            assertThat(result.size(), is(2));
            assertThat(result.get(0).getType(), is("image"));
            assertThat(result.get(0).getTitles().getCanonical(),
                    is("File:Flag_of_the_United_States.svg"));
            assertThat(result.get(0).getThumbnail().getSource(),
                    is("http://upload.wikimedia.org/wikipedia/en/thumb/a/a4/Flag_of_the_United_States.svg/320px-Flag_of_the_United_States.svg.png"));
            assertThat(result.get(0).getThumbnailUrl(),
                    is("http://upload.wikimedia.org/wikipedia/en/thumb/a/a4/Flag_of_the_United_States.svg/320px-Flag_of_the_United_States.svg.png"));
            assertThat(result.get(0).getPreferredSizedImageUrl(),
                    is("http://upload.wikimedia.org/wikipedia/en/thumb/a/a4/Flag_of_the_United_States.svg/1280px-Flag_of_the_United_States.svg.png"));
        }, throwable -> assertTrue(false));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestVideoSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        getRestService().getMedia("foo").subscribe(gallery -> {
            List<GalleryItem> result = gallery.getItems("video");

            assertThat(result.size(), is(1));
            assertThat(result.get(0).getType(), is("video"));
            assertThat(result.get(0).getTitles().getCanonical(),
                    is("File:Curiosity's_Seven_Minutes_of_Terror.ogv"));
            assertThat(result.get(0).getFilePage(),
                    is("https://commons.wikimedia.org/wiki/File:Curiosity%27s_Seven_Minutes_of_Terror.ogv"));
            assertThat(result.get(0).getSources().size(), is(6));
            assertThat(result.get(0).getOriginalVideoSource().getOriginalUrl(),
                    is("https://upload.wikimedia.org/wikipedia/commons/transcoded/9/96/Curiosity%27s_Seven_Minutes_of_Terror.ogv/Curiosity%27s_Seven_Minutes_of_Terror.ogv.720p.webm"));
        }, throwable -> assertTrue(false));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAudioSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        getRestService().getMedia("foo").subscribe(gallery -> {
            List<GalleryItem> result = gallery.getItems("audio");

            assertThat(result.size(), is(2));
            assertThat(result.get(0).getType(), is("audio"));
            assertThat(result.get(1).getTitles().getCanonical(),
                    is("File:March,_Colonel_John_R._Bourgeois,_Director_·_John_Philip_Sousa_·_United_States_Marine_Band.ogg"));
            assertThat(result.get(1).getDuration(), is(226.51766666667));
            assertThat(result.get(0).getAudioType(), is("generic"));
        }, throwable -> assertTrue(false));
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        getRestService().getMedia("foo").subscribe(gallery -> assertTrue(false),
                throwable -> assertTrue(throwable instanceof HttpStatusException));
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        getRestService().getMedia("foo").subscribe(gallery -> assertTrue(false),
                throwable -> assertTrue(throwable instanceof MalformedJsonException));
    }
}
