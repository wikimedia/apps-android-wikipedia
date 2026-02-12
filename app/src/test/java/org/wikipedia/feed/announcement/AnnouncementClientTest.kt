package org.wikipedia.feed.announcement

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wikipedia.json.JsonUtil
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
        announcementList = JsonUtil.decodeFromString(json)!!
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile(ANNOUNCEMENT_JSON_FILE)
        runBlocking {
            getAnnouncement()
        }.run {
            assertEquals(8, items.size)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestMalformed() {
        enqueueMalformed()
        runBlocking {
            try {
                getAnnouncement()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestNotFound() {
        enqueue404()
        runBlocking {
            try {
                getAnnouncement()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    fun testFundraisingParams() {
        val announcement = announcementList.items[ANNOUNCEMENT_FUNDRAISING_ANDROID]
        assertTrue(announcement.hasAction())
        assertTrue(announcement.hasFooterCaption())
        assertTrue(announcement.hasImageUrl())
    }

    @Test
    @Throws(Throwable::class)
    fun testShouldShowByCountry() {
        val announcement = announcementList.items[ANNOUNCEMENT_SURVEY_ANDROID]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        assertTrue(AnnouncementClient.shouldShow(announcement, "US", dateDuring))
        assertFalse(AnnouncementClient.shouldShow(announcement, "FI", dateDuring))
        assertFalse(AnnouncementClient.shouldShow(announcement, null, dateDuring))
    }

    @Test
    @Throws(Throwable::class)
    fun testShouldShowByDate() {
        val announcement = announcementList.items[ANNOUNCEMENT_SURVEY_ANDROID]
        val dateBefore = dateFormat.parse("2016-08-01")!!
        val dateAfter = dateFormat.parse("2017-01-05")!!
        assertFalse(AnnouncementClient.shouldShow(announcement, "US", dateBefore))
        assertFalse(AnnouncementClient.shouldShow(announcement, "US", dateAfter))
    }

    @Test
    @Throws(Throwable::class)
    fun testShouldShowByPlatform() {
        val announcementIOS = announcementList.items[ANNOUNCEMENT_IOS]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        assertFalse(AnnouncementClient.shouldShow(announcementIOS, "US", dateDuring))
    }

    @Test
    fun testShouldShowForInvalidDates() {
        assertNotNull(announcementList.items[ANNOUNCEMENT_INVALID_DATES])
        assertNotNull(announcementList.items[ANNOUNCEMENT_NO_DATES])
    }

    @Test
    @Throws(Throwable::class)
    fun testShouldShowForInvalidCountries() {
        val announcement = announcementList.items[ANNOUNCEMENT_NO_COUNTRIES]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        assertFalse(AnnouncementClient.shouldShow(announcement, "US", dateDuring))
        assertFalse(AnnouncementClient.shouldShow(announcement, "FI", dateDuring))
        assertFalse(AnnouncementClient.shouldShow(announcement, "", dateDuring))
    }

    @Test
    @Throws(Throwable::class)
    fun testBetaWithVersion() {
        val announcement = announcementList.items[ANNOUNCEMENT_BETA_WITH_VERSION]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        assertTrue(AnnouncementClient.shouldShow(announcement, "US", dateDuring))
        assertEquals(200, announcement.minVersion())
        assertEquals(10000, announcement.maxVersion())
    }

    @Test
    @Throws(Throwable::class)
    fun testForOldVersion() {
        val announcement = announcementList.items[ANNOUNCEMENT_FOR_OLD_VERSION]
        val dateDuring = dateFormat.parse("2016-11-20")!!
        assertFalse(AnnouncementClient.shouldShow(announcement, "US", dateDuring))
    }

    private suspend fun getAnnouncement(): AnnouncementList {
        return restService.getAnnouncements()
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
