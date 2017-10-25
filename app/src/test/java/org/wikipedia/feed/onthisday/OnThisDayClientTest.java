package org.wikipedia.feed.onthisday;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockWebServerTest;
import org.wikipedia.test.TestFileUtil;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OnThisDayClientTest extends MockWebServerTest {
    private OnThisDay onThisDay;
    @NonNull private OnThisDayClient client = new OnThisDayClient();

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
        String json = TestFileUtil.readRawFile("onthisday_02_06.json");
        onThisDay = GsonUnmarshaller.unmarshal(OnThisDay.class, json);
    }

    @Test
    public void testRequestSuccess() throws Throwable {
        enqueueFromFile("onthisday_02_06.json");
        FeedClient.Callback cb = mock(FeedClient.Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb).success(anyListOf(Card.class));
        verify(cb, never()).error(any(Throwable.class));
    }

    @Test
    public void testRequestMalFormed() throws Throwable {
        server().enqueue("Malformed.");
        FeedClient.Callback cb = mock(FeedClient.Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Card.class));
        verify(cb).error(any(Throwable.class));
    }

    @Test
    public void testRequestNotFound() throws Throwable {
        enqueue404();
        FeedClient.Callback cb = mock(FeedClient.Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Card.class));
        verify(cb).error(any(Throwable.class));
    }

    @Test
    public void testParamsNullity() throws Throwable {
        OnThisDay.Event event = onThisDay.events().get(0);
        assertThat((event.text() != null), is(true));
        assertThat((event.pages() != null), is(true));
    }

    private void request(@NonNull FeedClient.Callback cb) {
        Call<OnThisDay> call = client.request(service(OnThisDayClient.Service.class));
        call.enqueue(new OnThisDayClient.CallbackAdapter(cb, null, null));
    }
}
