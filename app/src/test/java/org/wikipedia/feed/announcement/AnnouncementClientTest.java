package org.wikipedia.feed.announcement;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.feed.dataclient.FeedClient.Callback;
import org.wikipedia.feed.model.Card;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockWebServerTest;
import org.wikipedia.test.TestFileUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AnnouncementClientTest extends MockWebServerTest {
    private static final int ANNOUNCEMENT_IOS = 0;
    private static final int ANNOUNCEMENT_SURVEY_ANDROID = 1;
    private static final int ANNOUNCEMENT_FUNDRAISING_ANDROID = 2;
    private static final int ANNOUNCEMENT_INVALID_DATES = 3;
    private static final int ANNOUNCEMENT_NO_DATES = 4;
    private static final int ANNOUNCEMENT_NO_COUNTRIES = 5;
    @NonNull private AnnouncementClient client = new AnnouncementClient();
    private AnnouncementList announcementList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
        String json = TestFileUtil.readRawFile("announce_2016_11_21.json");
        announcementList = GsonUnmarshaller.unmarshal(AnnouncementList.class, json);
    }

    @Test public void testRequestSuccess() throws Throwable {
        enqueueFromFile("announce_2016_11_21.json");
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb).success(anyListOf(Card.class));
        //noinspection unchecked
        verify(cb, never()).error(any(Throwable.class));
    }

    @Test public void testRequestMalformed() throws Throwable {
        server().enqueue("Jimmy crack corn, and I don't care.");
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Card.class));
        verify(cb).error(any(Throwable.class));
    }

    @Test public void testRequestNotFound() throws Throwable {
        enqueue404();
        Callback cb = mock(Callback.class);
        request(cb);
        server().takeRequest();
        verify(cb, never()).success(anyListOf(Card.class));
        verify(cb).error(any(Throwable.class));
    }

    @Test public void testFundraisingParams() throws Throwable {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_FUNDRAISING_ANDROID);
        assertThat(announcement.hasAction(), is(true));
        assertThat(announcement.hasFooterCaption(), is(true));
        assertThat(announcement.hasImageUrl(), is(true));
    }

    @Test public void testShouldShowByCountry() throws Throwable {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_SURVEY_ANDROID);
        Date dateDuring = dateFormat.parse("2016-11-20");
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(true));
        assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, null, dateDuring), is(false));
    }

    @Test public void testShouldShowByDate() throws Throwable {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_SURVEY_ANDROID);
        Date dateBefore = dateFormat.parse("2016-08-01");
        Date dateAfter = dateFormat.parse("2017-01-05");
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateBefore), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateAfter), is(false));
    }

    @Test public void testShouldShowByPlatform() throws Throwable {
        Announcement announcementIOS = announcementList.items().get(ANNOUNCEMENT_IOS);
        Date dateDuring = dateFormat.parse("2016-11-20");
        assertThat(AnnouncementClient.shouldShow(announcementIOS, "US", dateDuring), is(false));
    }

    @Test public void testShouldShowForInvalidDates() throws Throwable {
        assertThat(announcementList.items().get(ANNOUNCEMENT_INVALID_DATES), is(nullValue()));
        assertThat(announcementList.items().get(ANNOUNCEMENT_NO_DATES), is(nullValue()));
    }

    @Test public void testShouldShowForInvalidCountries() throws Throwable {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_NO_COUNTRIES);
        Date dateDuring = dateFormat.parse("2016-11-20");
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "", dateDuring), is(false));
    }

    private void request(@NonNull Callback cb) {
        Call<AnnouncementList> call = client.request(service(AnnouncementClient.Service.class));
        call.enqueue(new AnnouncementClient.CallbackAdapter(cb));
    }
}
