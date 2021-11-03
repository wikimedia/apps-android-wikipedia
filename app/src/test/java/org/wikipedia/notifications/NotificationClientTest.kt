package org.wikipedia.notifications

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
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
            MatcherAssert.assertThat(firstNotification.category, Matchers.`is`(NotificationCategory.EDIT_THANK.id))
            MatcherAssert.assertThat(firstNotification.title?.full, Matchers.`is`("PageTitle"))
            MatcherAssert.assertThat(firstNotification.agent?.name, Matchers.`is`("User1"))
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
                MatcherAssert.assertThat(e, Matchers.notNullValue())
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun testNotificationReverted() {
        val json = TestFileUtil.readRawFile("notification_revert.json")
        val n = JsonUtil.decodeFromString<Notification>(json)!!
        MatcherAssert.assertThat(n.type, Matchers.`is`(NotificationCategory.REVERTED.id))
        MatcherAssert.assertThat(n.wiki, Matchers.`is`("wikidatawiki"))
        MatcherAssert.assertThat(n.agent?.name, Matchers.`is`("User1"))
        MatcherAssert.assertThat(n.isFromWikidata, Matchers.`is`(true))
    }

    @Test
    @Throws(Throwable::class)
    fun testNotificationMention() {
        enqueueFromFile("notification_mention.json")

        runBlocking {
            allNotification()
        }.run {
            val notifications = query?.notifications?.list
            MatcherAssert.assertThat(notifications?.get(0)?.category, Matchers.startsWith(NotificationCategory.MENTION.id))
            MatcherAssert.assertThat(notifications?.get(1)?.category, Matchers.startsWith(NotificationCategory.MENTION.id))
            MatcherAssert.assertThat(notifications?.get(2)?.category, Matchers.startsWith(NotificationCategory.MENTION.id))
        }
    }

    private suspend fun allNotification(): MwQueryResponse {
        return apiService.getAllNotifications("*", "!read", null)
    }
}
