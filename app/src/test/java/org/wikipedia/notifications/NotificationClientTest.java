package org.wikipedia.notifications;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;

public class NotificationClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("notifications.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> {
                    List<Notification> notifications = response.getQuery().getNotifications().getList();
                    return notifications.get(0).category().equals(NotificationCategory.EDIT_THANK.getId())
                            && notifications.get(0).title().full().equals("PageTitle")
                            && notifications.get(0).agent().name().equals("User1");
                });
    }

    @Test public void testRequestMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }

    @Test public void testNotificationReverted() throws Throwable {
        String json = TestFileUtil.readRawFile("notification_revert.json");
        Notification n = GsonUnmarshaller.unmarshal(Notification.class, json);
        assertThat(n.type(), is(NotificationCategory.REVERTED.getId()));
        assertThat(n.wiki(), is("wikidatawiki"));
        assertThat(n.agent().name(), is("User1"));
        assertThat(n.isFromWikidata(), is(true));
    }

    @Test public void testNotificationMention() throws Throwable {
        enqueueFromFile("notification_mention.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> {
                    List<Notification> notifications = response.getQuery().getNotifications().getList();
                    return notifications.get(0).category().startsWith(NotificationCategory.MENTION.getId())
                            && notifications.get(1).category().startsWith(NotificationCategory.MENTION.getId())
                            && notifications.get(2).category().startsWith(NotificationCategory.MENTION.getId());
                });
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getAllNotifications("*", "!read", null);
    }
}
