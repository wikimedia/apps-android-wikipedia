package org.wikipedia.feed.announcement;

import com.google.gson.stream.MalformedJsonException;

import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.test.MockRetrofitTest;
import org.wikipedia.test.TestFileUtil;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;

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
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

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

        TestObserver<AnnouncementList> observer = new TestObserver<>();
        getObservable().subscribe(observer);

        observer.assertComplete().assertNoErrors()
                .assertValue(list -> list.items().size() == 8);
    }

    @Test public void testRequestMalformed() {
        enqueueMalformed();
        TestObserver<AnnouncementList> observer = new TestObserver<>();
        getObservable().subscribe(observer);
        observer.assertError(MalformedJsonException.class);
    }

    @Test public void testRequestNotFound() {
        enqueue404();
        TestObserver<AnnouncementList> observer = new TestObserver<>();
        getObservable().subscribe(observer);
        observer.assertError(Exception.class);
    }

    @Test public void testFundraisingParams() {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_FUNDRAISING_ANDROID);
        assertThat(announcement.hasAction(), is(true));
        assertThat(announcement.hasFooterCaption(), is(true));
        assertThat(announcement.hasImageUrl(), is(true));
    }

    @Test public void testShouldShowByCountry() {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_SURVEY_ANDROID);
        LocalDate dateDuring = LocalDate.from(dateFormatter.parse("2016-11-20"));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(true));
        assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, null, dateDuring), is(false));
    }

    @Test public void testShouldShowByDate() {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_SURVEY_ANDROID);
        LocalDate dateBefore = LocalDate.from(dateFormatter.parse("2016-08-01"));
        LocalDate dateAfter = LocalDate.from(dateFormatter.parse("2017-01-05"));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateBefore), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateAfter), is(false));
    }

    @Test public void testShouldShowByPlatform() {
        Announcement announcementIOS = announcementList.items().get(ANNOUNCEMENT_IOS);
        LocalDate dateDuring = LocalDate.from(dateFormatter.parse("2016-11-20"));
        assertThat(AnnouncementClient.shouldShow(announcementIOS, "US", dateDuring), is(false));
    }

    @Test public void testShouldShowForInvalidDates() {
        assertThat(announcementList.items().get(ANNOUNCEMENT_INVALID_DATES), is(nullValue()));
        assertThat(announcementList.items().get(ANNOUNCEMENT_NO_DATES), is(nullValue()));
    }

    @Test public void testShouldShowForInvalidCountries() {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_NO_COUNTRIES);
        LocalDate dateDuring = LocalDate.from(dateFormatter.parse("2016-11-20"));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), is(false));
        assertThat(AnnouncementClient.shouldShow(announcement, "", dateDuring), is(false));
    }

    @Test public void testBetaWithVersion() {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_BETA_WITH_VERSION);
        LocalDate dateDuring = LocalDate.from(dateFormatter.parse("2016-11-20"));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(true));
    }

    @Test public void testForOldVersion() {
        Announcement announcement = announcementList.items().get(ANNOUNCEMENT_FOR_OLD_VERSION);
        LocalDate dateDuring = LocalDate.from(dateFormatter.parse("2016-11-20"));
        assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), is(false));
    }

    private Observable<AnnouncementList> getObservable() {
        return getRestService().getAnnouncements();
    }
}
