package org.wikipedia.notifications;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockWebServerTest;
import org.wikipedia.test.TestFileUtil;
import org.wikipedia.wikidata.EntityClient;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class NotificationClientTest extends MockWebServerTest {
    @NonNull private NotificationClient client = NotificationClient.instance();

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("notifications.json");
        NotificationClient.Callback cb = mock(NotificationClient.Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb).success(anyListOf(Notification.class));
        //noinspection unchecked
        verify(cb, never()).failure(any(Throwable.class));
    }

    @Test public void testRequestMalformed() throws Throwable {
        server().enqueue("(╯°□°）╯︵ ┻━┻");
        NotificationClient.Callback cb = mock(NotificationClient.Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Notification.class));
        verify(cb).failure(any(Throwable.class));
    }

    @Test public void testNotificationReverted() throws Throwable {
        String json = TestFileUtil.readRawFile("notification_revert.json");
        Notification n = GsonUnmarshaller.unmarshal(Notification.class, json);
        assertThat(n.type(), is(Notification.TYPE_REVERTED));
        assertThat(n.wiki(), is("wikidatawiki"));
        assertThat(n.agent().name(), is("User1"));
        assertThat(n.isFromWikidata(), is(true));
    }

    private void request(@NonNull final NotificationClient.Callback cb) {
        Call<MwQueryResponse> call = client.requestNotifications((service(NotificationClient.Service.class)),
                EntityClient.WIKIDATA_WIKI);
        call.enqueue(new NotificationClient.CallbackAdapter(cb));
    }
}
