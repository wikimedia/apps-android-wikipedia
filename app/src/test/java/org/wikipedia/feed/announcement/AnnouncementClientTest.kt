package org.wikipedia.feed.announcement

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.MockRetrofitTest
import org.wikipedia.test.TestFileUtil
import java.time.LocalDate

class AnnouncementClientTest : MockRetrofitTest() {
    private lateinit var announcementList: AnnouncementList

    @Before
    override fun setUp() {
        super.setUp()
        val json = TestFileUtil.readRawFile(ANNOUNCEMENT_JSON_FILE)
        announcementList = JsonUtil.decodeFromString(json)!!
    }

    @Test
    fun testRequestSuccess() {
        enqueueFromFile(ANNOUNCEMENT_JSON_FILE)
        restService.announcements.test().await()
            .assertComplete()
            .assertNoErrors()
            .assertValue { it.items.size == 8 }
    }

    @Test
    fun testRequestMalformed() {
        enqueueMalformed()
        restService.announcements.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    fun testRequestNotFound() {
        enqueue404()
        restService.announcements.test().await()
            .assertError(Exception::class.java)
    }

    @Test
    fun testFundraisingParams() {
        val announcement = announcementList.items[ANNOUNCEMENT_FUNDRAISING_ANDROID]
        MatcherAssert.assertThat(announcement.hasAction(), Matchers.`is`(true))
        MatcherAssert.assertThat(announcement.hasFooterCaption(), Matchers.`is`(true))
        MatcherAssert.assertThat(announcement.hasImageUrl(), Matchers.`is`(true))
    }

    @Test
    fun testShouldShowByCountry() {
        val announcement = announcementList.items[ANNOUNCEMENT_SURVEY_ANDROID]
        val dateDuring = LocalDate.of(2016, 11, 20)
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(true))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, null, dateDuring), Matchers.`is`(false))
    }

    @Test
    fun testShouldShowByDate() {
        val announcement = announcementList.items[ANNOUNCEMENT_SURVEY_ANDROID]
        val dateBefore = LocalDate.of(2016, 8, 1)
        val dateAfter = LocalDate.of(2017, 1, 5)
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateBefore), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateAfter), Matchers.`is`(false))
    }

    @Test
    fun testShouldShowByPlatform() {
        val announcementIOS = announcementList.items[ANNOUNCEMENT_IOS]
        val dateDuring = LocalDate.of(2016, 11, 20)
        MatcherAssert.assertThat(
            AnnouncementClient.shouldShow(announcementIOS, "US", dateDuring),
            Matchers.`is`(false)
        )
    }

    @Test
    fun testShouldShowForInvalidDates() {
        MatcherAssert.assertThat(announcementList.items[ANNOUNCEMENT_INVALID_DATES], Matchers.`is`(Matchers.notNullValue()))
        MatcherAssert.assertThat(announcementList.items[ANNOUNCEMENT_NO_DATES], Matchers.`is`(Matchers.notNullValue()))
    }

    @Test
    fun testShouldShowForInvalidCountries() {
        val announcement = announcementList.items[ANNOUNCEMENT_NO_COUNTRIES]
        val dateDuring = LocalDate.of(2016, 11, 20)
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "", dateDuring), Matchers.`is`(false))
    }

    @Test
    fun testBetaWithVersion() {
        val announcement = announcementList.items[ANNOUNCEMENT_BETA_WITH_VERSION]
        val dateDuring = LocalDate.of(2016, 11, 20)
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(true))
        MatcherAssert.assertThat(announcement.minVersion(), Matchers.`is`(200))
        MatcherAssert.assertThat(announcement.maxVersion(), Matchers.`is`(10000))
    }

    @Test
    fun testForOldVersion() {
        val announcement = announcementList.items[ANNOUNCEMENT_FOR_OLD_VERSION]
        val dateDuring = LocalDate.of(2016, 11, 20)
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(false))
    }

    companion object {
        private const val ANNOUNCEMENT_IOS = 0
        private const val ANNOUNCEMENT_SURVEY_ANDROID = 1
        private const val ANNOUNCEMENT_FUNDRAISING_ANDROID = 2
        private const val ANNOUNCEMENT_INVALID_DATES = 3
        private const val ANNOUNCEMENT_NO_DATES = 4
        private const val ANNOUNCEMENT_NO_COUNTRIES = 5
        private const val ANNOUNCEMENT_BETA_WITH_VERSION = 6
        private const val ANNOUNCEMENT_FOR_OLD_VERSION = 7
        private const val ANNOUNCEMENT_JSON_FILE = "announce_2016_11_21.json"
    }
}
