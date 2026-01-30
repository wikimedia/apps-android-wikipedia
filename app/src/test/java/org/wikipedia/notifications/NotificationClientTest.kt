package org.wikipedia.notifications

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.test.MockRetrofitTest
import org.wikipedia.test.TestFileUtil

class NotificationClientTest : MockRetrofitTest() {

    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("notifications.json")
        runBlocking {
            allNotification()
        }.run {
            val firstNotification = query?.notifications?.list?.first()!!
            assertEquals(NotificationCategory.EDIT_THANK.id, firstNotification.category)
            assertEquals("PageTitle", firstNotification.title?.full)
            assertEquals("User1", firstNotification.agent?.name)
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestMalformed() {
        enqueueMalformed()
        runBlocking {
            try {
                allNotification()
            } catch (e: Exception) {
                assertNotNull(e)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNotificationReverted() {
        val json = TestFileUtil.readRawFile("notification_revert.json")
        val n = JsonUtil.decodeFromString<Notification>(json)!!
        assertEquals(NotificationCategory.REVERTED.id, n.type)
        assertEquals("wikidatawiki", n.wiki)
        assertEquals("User1", n.agent?.name)
        assertTrue(n.isFromWikidata)
    }

    @Test
    @Throws(Throwable::class)
    fun testNotificationMention() {
        enqueueFromFile("notification_mention.json")

        runBlocking {
            allNotification()
        }.run {
            val notifications = query?.notifications?.list
            assertTrue(notifications?.get(0)?.category?.startsWith(NotificationCategory.MENTION.id) == true)
            assertTrue(notifications?.get(1)?.category?.startsWith(NotificationCategory.MENTION.id) == true)
            assertTrue(notifications?.get(2)?.category?.startsWith(NotificationCategory.MENTION.id) == true)
        }
    }

    private suspend fun allNotification(): MwQueryResponse {
        return apiService.getAllNotifications("*", "!read", null)
    }
}
