package org.wikipedia.notifications;

import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.json.MoshiUtil;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import java.io.IOException;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.wikipedia.notifications.Notification.CATEGORY_EDIT_THANK;
import static org.wikipedia.notifications.Notification.CATEGORY_MENTION;

public class NotificationClientTest extends MockRetrofitTest {

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("notifications.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> {
                    List<Notification> notifications = response.getQuery().getNotifications().getList();
                    return notifications.get(0).getCategory().equals(CATEGORY_EDIT_THANK)
                            && notifications.get(0).getTitle().getFull().equals("PageTitle")
                            && notifications.get(0).getAgent().getName().equals("User1");
                });
    }

    @Test public void testRequestMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(IOException.class);
    }

    @Test public void testNotificationReverted() throws Throwable {
        final String json = TestFileUtil.readRawFile("notification_revert.json");
        Notification n = MoshiUtil.getDefaultMoshi().adapter(Notification.class).fromJson(json);
        assertThat(n.getType(), is(Notification.CATEGORY_REVERTED));
        assertThat(n.getWiki(), is("wikidatawiki"));
        assertThat(n.getAgent().getName(), is("User1"));
        assertThat(n.isFromWikidata(), is(true));
    }

    @Test public void testNotificationMention() throws Throwable {
        enqueueFromFile("notification_mention.json");
        getObservable().test().await()
                .assertComplete().assertNoErrors()
                .assertValue(response -> {
                    List<Notification> notifications = response.getQuery().getNotifications().getList();
                    return notifications.get(0).getCategory().startsWith(CATEGORY_MENTION)
                            && notifications.get(1).getCategory().startsWith(CATEGORY_MENTION)
                            && notifications.get(2).getCategory().startsWith(CATEGORY_MENTION);
                });
    }

    private Observable<MwQueryResponse> getObservable() {
        return getApiService().getAllNotifications("*", "!read", null);
    }
}
