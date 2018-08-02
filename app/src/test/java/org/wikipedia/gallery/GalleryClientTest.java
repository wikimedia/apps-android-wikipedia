package org.wikipedia.gallery;

import android.support.annotation.NonNull;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.test.MockWebServerTest;

import java.util.List;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GalleryClientTest extends MockWebServerTest {
    private static final String RAW_JSON_FILE = "gallery.json";

    @NonNull private final GalleryClient client = new GalleryClient();

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAllSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb);

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<GalleryItem> result = captor.getValue();

        assertThat(result != null, is(true));
        assertThat(result.size(), is(5));
        assertThat(result.get(0).getType(), is("image"));
        assertThat(result.get(2).getType(), is("audio"));
        assertThat(result.get(4).getType(), is("video"));

    }

    @Test
    public void testRequestImageSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb, "image");

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<GalleryItem> result = captor.getValue();

        assertThat(result != null, is(true));
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
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestVideoSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb, "video");

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<GalleryItem> result = captor.getValue();

        assertThat(result != null, is(true));
        assertThat(result.size(), is(1));
        assertThat(result.get(0).getType(), is("video"));
        assertThat(result.get(0).getTitles().getCanonical(),
                is("File:Curiosity's_Seven_Minutes_of_Terror.ogv"));
        assertThat(result.get(0).getFilePage(),
                is("https://commons.wikimedia.org/wiki/File:Curiosity%27s_Seven_Minutes_of_Terror.ogv"));
        assertThat(result.get(0).getSources().size(), is(6));
        assertThat(result.get(0).getOriginalVideoSource().getOriginalUrl(),
                is("https://upload.wikimedia.org/wikipedia/commons/transcoded/9/96/Curiosity%27s_Seven_Minutes_of_Terror.ogv/Curiosity%27s_Seven_Minutes_of_Terror.ogv.720p.webm"));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestAudioSuccess() throws Throwable {
        enqueueFromFile(RAW_JSON_FILE);

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb, "audio");

        server().takeRequest();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(cb).success(eq(call), captor.capture());

        List<GalleryItem> result = captor.getValue();

        assertThat(result != null, is(true));
        assertThat(result.size(), is(2));
        assertThat(result.get(0).getType(), is("audio"));
        assertThat(result.get(1).getTitles().getCanonical(),
                is("File:March,_Colonel_John_R._Bourgeois,_Director_·_John_Philip_Sousa_·_United_States_Marine_Band.ogg"));
        assertThat(result.get(1).getDuration(), is(226.51766666667));
        assertThat(result.get(0).getAudioType(), is("generic"));
    }

    @Test public void testRequestResponseApiError() throws Throwable {
        enqueueFromFile("api_error.json");

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, Throwable.class);
    }

    @Test public void testRequestResponseFailure() throws Throwable {
        enqueue404();

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, Throwable.class);
    }

    @Test public void testRequestResponseMalformed() throws Throwable {
        server().enqueue("'");

        GalleryClient.Callback cb = mock(GalleryClient.Callback.class);
        Call<Gallery> call = request(cb);

        server().takeRequest();
        assertCallbackFailure(call, cb, MalformedJsonException.class);
    }

    private void assertCallbackFailure(@NonNull Call<Gallery> call,
                                       @NonNull GalleryClient.Callback cb,
                                       @NonNull Class<? extends Throwable> throwable) {
        //noinspection unchecked
        verify(cb, never()).success(any(Call.class), any(List.class));
        verify(cb).failure(eq(call), isA(throwable));
    }

    private Call<Gallery> request(@NonNull GalleryClient.Callback cb, @NonNull String...types) {
        return client.request(service(GalleryClient.Service.class), "test", cb, types);
    }
}
