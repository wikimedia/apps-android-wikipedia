package org.wikipedia.feed.announcement;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private static final String ANNOUNCEMENT_JSON_FILE = "announce_2016_11_21.json";

    @SuppressWarnings("checkstyle:magicnumber")
    private static final LocalDateTime LOCAL_DATE_TIME = LocalDate.of(2016, 11, 20).atStartOfDay();

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
                .assertValue(list -> list.getItems().size() == 8);
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
        final Announcement announcement = announcementList.getItems().get(ANNOUNCEMENT_FUNDRAISING_ANDROID);
        assertThat(announcement.hasAction(), is(true));
        assertThat(announcement.hasFooterCaption(), is(true));
        assertThat(announcement.hasImageUrl(), is(true));
    }

    @Test public void testShouldShowByCountry() {
        final Announcement announcement = announcementList.getItems().get(ANNOUNCEMENT_SURVEY_ANDROID);
        assertThat(AnnouncementClient.shouldShow(announcement, "US", LOCAL_DATE_TIME), is(true));
        assertThat(AnnouncementClient.shouldShow(announcement, "FI", LOCAL_DATE_TIME), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, null, LOCAL_DATE_TIME), is(false));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test public void testShouldShowByDate() {
        final Announcement announcement = announcementList.getItems().get(ANNOUNCEMENT_SURVEY_ANDROID);
        final LocalDateTime dateBefore = LocalDate.of(2016, 8, 1).atStartOfDay();
        final LocalDateTime dateAfter = LocalDate.of(2017, 1, 5).atStartOfDay();
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateBefore), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateAfter), is(false));
    }

    @Test public void testShouldShowByPlatform() {
        final Announcement announcementIOS = announcementList.getItems().get(ANNOUNCEMENT_IOS);
        assertThat(AnnouncementClient.shouldShow(announcementIOS, "US", LOCAL_DATE_TIME), is(false));
    }

    @Test public void testShouldShowForInvalidDates() {
        assertThat(announcementList.getItems().get(ANNOUNCEMENT_INVALID_DATES), is(nullValue()));
        assertThat(announcementList.getItems().get(ANNOUNCEMENT_NO_DATES), is(nullValue()));
    }

    @Test public void testShouldShowForInvalidCountries() {
        final Announcement announcement = announcementList.getItems().get(ANNOUNCEMENT_NO_COUNTRIES);
        assertThat(AnnouncementClient.shouldShow(announcement, "US", LOCAL_DATE_TIME), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "FI", LOCAL_DATE_TIME), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "", LOCAL_DATE_TIME), is(false));
    }

    @Test public void testBetaWithVersion() {
        final Announcement announcement = announcementList.getItems().get(ANNOUNCEMENT_BETA_WITH_VERSION);
        assertThat(AnnouncementClient.shouldShow(announcement, "US", LOCAL_DATE_TIME), is(true));
    }

    @Test public void testForOldVersion() {
        final Announcement announcement = announcementList.getItems().get(ANNOUNCEMENT_FOR_OLD_VERSION);
        assertThat(AnnouncementClient.shouldShow(announcement, "US", LOCAL_DATE_TIME), is(false));
    }

    private Observable<AnnouncementList> getObservable() {
        return getRestService().getAnnouncements();
    }
}
