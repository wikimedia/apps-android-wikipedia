package org.wikipedia.notifications;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.notifications.Notification.CATEGORY_EDIT_THANK;
import static org.wikipedia.notifications.Notification.CATEGORY_MENTION;

public class NotificationClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("notifications.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(response -> {
                    List<Notification> notifications = response.query().notifications().list();
                    return notifications.get(0).category().equals(CATEGORY_EDIT_THANK)
                            && notifications.get(0).title().full().equals("PageTitle")
                            && notifications.get(0).agent().name().equals("User1");
                });
    }

    @Test public void testRequestMalformed() {
        enqueueMalformed();
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertError(MalformedJsonException.class);
    }

    @Test public void testNotificationReverted() throws Throwable {
        String json = TestFileUtil.readRawFile("notification_revert.json");
        Notification n = GsonUnmarshaller.unmarshal(Notification.class, json);
        assertThat(n.type(), is(Notification.CATEGORY_REVERTED));
        assertThat(n.wiki(), is("wikidatawiki"));
        assertThat(n.agent().name(), is("User1"));
        assertThat(n.isFromWikidata(), is(true));
    }

    @Test public void testNotificationMention() throws Throwable {
        enqueueFromFile("notification_mention.json");
        TestObserver<MwQueryResponse> observer = new TestObserver<>();

        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(response -> {
                    List<Notification> notifications = response.query().notifications().list();
                    return notifications.get(0).category().startsWith(CATEGORY_MENTION)
                            && notifications.get(1).category().startsWith(CATEGORY_MENTION)
                            && notifications.get(2).category().startsWith(CATEGORY_MENTION);
                });
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getAllNotifications("*", "!read", null);
    }
}
