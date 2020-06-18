package org.wikipedia.feed.announcement;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.rxjava3.core.Observable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AnnouncementClientTest extends MockRetrofitTest {
    private static final int ANNOUNCEMENT_IOS = 0;
    private static final int ANNOUNCEMENT_SURVEY_ANDROID = 1;
    private static final int ANNOUNCEMENT_FUNDRAISING_ANDROID = 2;
    private static final int ANNOUNCEMENT_INVALID_DATES = 3;
    private static final int ANNOUNCEMENT_NO_DATES = 4;
    private static final int ANNOUNCEMENT_NO_COUNTRIES = 5;
    private static final int ANNOUNCEMENT_BETA_WITH_VERSION = 6;
    private static final int ANNOUNCEMENT_FOR_OLD_VERSION = 7;
    private AnnouncementList announcementList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    private static final String ANNOUNCEMENT_JSON_FILE = "announce_2016_11_21.json";

    @Before
    @Override
    public void setUp() throws Throwable {
        super.setUp();
        String json = TestFileUtil.readRawFile(ANNOUNCEMENT_JSON_FILE);
        announcementList = GsonUnmarshaller.unmarshal(AnnouncementList.class, json);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testRequestSuccess() throws Throwable {
        enqueueFromFile(ANNOUNCEMENT_JSON_FILE);
        getObservable().test().await()
                .assertComplete()
                .assertNoErrors()
                .assertValue(list -> list.items().size() == 8);
    }

    @Test public void testRequestMalformed() throws Throwable {
        enqueueMalformed();
        getObservable().test().await()
                .assertError(MalformedJsonException.class);
    }

    @Test public void testRequestNotFound() throws Throwable {
        enqueue404();
        getObservable().test().await()
                .assertError(Exception.class);
    }

    @Test public void testFundraisingParams() {
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

    @Test public void testShouldShowForInvalidDates() {
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

    @Test public void testBetaWithVersion() throws Throwable {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_BETA_WITH_VERSION);
        Date dateDuring = dateFormat.parse("2016-11-20");
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(true));
    }

    @Test public void testForOldVersion() throws Throwable {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_FOR_OLD_VERSION);
        Date dateDuring = dateFormat.parse("2016-11-20");
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(false));
    }

    private Observable<AnnouncementList> getObservable() {
        return getRestService().getAnnouncements();
    }
}
