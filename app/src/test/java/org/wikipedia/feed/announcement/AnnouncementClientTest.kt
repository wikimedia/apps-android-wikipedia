package org.wikipedia.feed.announcement

import com.google.gson.stream.MalformedJsonException
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.test.MockRetrofitTest
import org.wikipedia.test.TestFileUtil
import java.text.SimpleDateFormat
import java.util.*

class AnnouncementClientTest : MockRetrofitTest() {
    private lateinit var announcementList: AnnouncementList
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    @Before
    @Throws(Throwable::class)
    override fun setUp() {
        super.setUp()
        val json = TestFileUtil.readRawFile(ANNOUNCEMENT_JSON_FILE)
        announcementList = GsonUnmarshaller.unmarshal(AnnouncementList::class.java, json)
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile(ANNOUNCEMENT_JSON_FILE)
        restService.announcements.test().await()
            .assertComplete()
            .assertNoErrors()
            .assertValue { it.items.size == 8 }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestMalformed() {
        enqueueMalformed()
        restService.announcements.test().await()
            .assertError(MalformedJsonException::class.java)
    }

    @Test
    @Throws(Throwable::class)
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
    @Throws(Throwable::class)
    fun testShouldShowByCountry() {
        val announcement = announcementList.items[ANNOUNCEMENT_SURVEY_ANDROID]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(true))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, null, dateDuring), Matchers.`is`(false))
    }

    @Test
    @Throws(Throwable::class)
    fun testShouldShowByDate() {
        val announcement = announcementList.items[ANNOUNCEMENT_SURVEY_ANDROID]
        val dateBefore = dateFormat.parse("2016-08-01")!!
        val dateAfter = dateFormat.parse("2017-01-05")!!
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateBefore), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateAfter), Matchers.`is`(false))
    }

    @Test
    @Throws(Throwable::class)
    fun testShouldShowByPlatform() {
        val announcementIOS = announcementList.items[ANNOUNCEMENT_IOS]
        val dateDuring = dateFormat.parse("2016-11-20")!!
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
    @Throws(Throwable::class)
    fun testShouldShowForInvalidCountries() {
        val announcement = announcementList.items[ANNOUNCEMENT_NO_COUNTRIES]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "FI", dateDuring), Matchers.`is`(false))
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "", dateDuring), Matchers.`is`(false))
    }

    @Test
    @Throws(Throwable::class)
    fun testBetaWithVersion() {
        val announcement = announcementList.items[ANNOUNCEMENT_BETA_WITH_VERSION]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        MatcherAssert.assertThat(AnnouncementClient.shouldShow(announcement, "US", dateDuring), Matchers.`is`(true))
    }

    @Test
    @Throws(Throwable::class)
    fun testForOldVersion() {
        val announcement = announcementList.items[ANNOUNCEMENT_FOR_OLD_VERSION]
        val dateDuring = dateFormat.parse("2016-11-20")!!
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
