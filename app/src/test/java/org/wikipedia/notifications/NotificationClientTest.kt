package org.wikipedia.notifications

import com.google.gson.stream.MalformedJsonException
import io.reactivex.rxjava3.core.Observable
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.test.MockRetrofitTest
import org.wikipedia.test.TestFileUtil

class NotificationClientTest : MockRetrofitTest() {
    @Test
    @Throws(Throwable::class)
    fun testRequestSuccess() {
        enqueueFromFile("notifications.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue {
                val firstNotification = it.query?.notifications?.list?.first()!!
                firstNotification.category == NotificationCategory.EDIT_THANK.id &&
                        firstNotification.title?.full == "PageTitle" &&
                        firstNotification.agent?.name == "User1"
            }
    }

    @Test
    @Throws(Throwable::class)
    fun testRequestMalformed() {
        enqueueMalformed()
        observable.test().await()
            .assertError(MalformedJsonException::class.java)
    }

    @Test
    @Throws(Throwable::class)
    fun testNotificationReverted() {
        val json = TestFileUtil.readRawFile("notification_revert.json")
        val n = GsonUnmarshaller.unmarshal(Notification::class.java, json)
        MatcherAssert.assertThat(n.type, Matchers.`is`(NotificationCategory.REVERTED.id))
        MatcherAssert.assertThat(n.wiki, Matchers.`is`("wikidatawiki"))
        MatcherAssert.assertThat(n.agent?.name, Matchers.`is`("User1"))
        MatcherAssert.assertThat(n.isFromWikidata, Matchers.`is`(true))
    }

    @Test
    @Throws(Throwable::class)
    fun testNotificationMention() {
        enqueueFromFile("notification_mention.json")
        observable.test().await()
            .assertComplete().assertNoErrors()
            .assertValue {
                val notifications = it.query?.notifications?.list
                notifications?.get(0)?.category?.startsWith(NotificationCategory.MENTION.id) == true &&
                        notifications[1].category.startsWith(NotificationCategory.MENTION.id) &&
                        notifications[2].category.startsWith(NotificationCategory.MENTION.id)
            }
    }

    private val observable: Observable<MwQueryResponse>
        get() = apiService.getAllNotifications("*", "!read", null)
}
