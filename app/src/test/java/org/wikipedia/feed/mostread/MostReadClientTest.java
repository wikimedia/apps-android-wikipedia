package org.wikipedia.feed.mostread;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wikipedia.Site;
import org.wikipedia.feed.FeedClient.Callback;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.mostread.MostReadClient.Service;
import org.wikipedia.test.MockWebServerTest;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("checkstyle:magicnumber")
public class MostReadClientTest extends MockWebServerTest {
    @NonNull private final MostReadClient subject = new MostReadClient();
    @NonNull private final Site site = new Site("en.wikipedia.org");

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("most_read.json");

        Service service = service(Service.class);
        Calendar date = calendar(2016, Calendar.JUNE, 1);
        Callback cb = mock(Callback.class);
        subject.request(service, site, date, cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, "2016/06/01");

        assertCallbackSuccess(cb, date);
    }

    @Test public void testRequestFailure() throws Throwable {
        enqueue404();

        Service service = service(Service.class);
        Calendar date = calendar(2016, Calendar.JUNE, 1);
        Callback cb = mock(Callback.class);
        subject.request(service, site, date, cb);

        RecordedRequest req = server().takeRequest();
        assertRequestIssued(req, "2016/06/01");

        verify(cb, never()).success(anyListOf(Card.class));
    }

    private void assertRequestIssued(@NonNull RecordedRequest req, @NonNull String date) {
        assertThat(req.getPath(), endsWith(date));
    }

    private void assertCallbackSuccess(@NonNull Callback cb, @NonNull Calendar date) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<MostReadListCard>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(cb).success(captor.capture());

        List<MostReadListCard> rsp = captor.getValue();
        assertThat(rsp, notNullValue());
        assertThat(rsp.size(), greaterThan(0));
        assertThat(rsp.get(0).date(), is(date.getTime()));
    }

    @NonNull private Calendar calendar(int year, int month, int day) throws Throwable {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar calendar = new GregorianCalendar(utc);
        calendar.clear();
        calendar.set(year, month, day);
        return calendar;
    }
}