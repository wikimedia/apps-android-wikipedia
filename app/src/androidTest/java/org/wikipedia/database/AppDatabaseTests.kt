package org.wikipedia.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.json.JsonUtil
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.search.db.RecentSearchDao
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.talk.db.TalkPageSeenDao
import java.util.*

@RunWith(AndroidJUnit4::class)
class AppDatabaseTests {
    private lateinit var db: AppDatabase
    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var talkPageSeenDao: TalkPageSeenDao
    private lateinit var notificationDao: NotificationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        recentSearchDao = db.recentSearchDao()
        talkPageSeenDao = db.talkPageSeenDao()
        notificationDao = db.notificationDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testRecentSearch() {
        val now = Date()

        recentSearchDao.insertRecentSearch(RecentSearch("Foo")).blockingSubscribe()
        recentSearchDao.insertRecentSearch(RecentSearch("Bar", now)).blockingSubscribe()

        var results = recentSearchDao.getRecentSearches().blockingGet()
        assertThat(results.size, equalTo(2))
        assertThat(results[0].text, equalTo("Foo"))
        assertThat(results[1].text, equalTo("Bar"))
        assertThat(results[1].timestamp, equalTo(now))

        recentSearchDao.insertRecentSearch(RecentSearch("Baz")).blockingSubscribe()
        results = recentSearchDao.getRecentSearches().blockingGet()
        assertThat(results.size, equalTo(3))

        recentSearchDao.deleteAll().blockingSubscribe()
        results = recentSearchDao.getRecentSearches().blockingGet()
        assertThat(results.size, equalTo(0))
    }

    @Test
    fun testTalkPageSeen() {
        talkPageSeenDao.insertTalkPageSeen(TalkPageSeen("328b389f2063da236be9d363b272eb0fa6e065816607099c7db8c09e1c919617"))
        talkPageSeenDao.insertTalkPageSeen(TalkPageSeen("5fbbb2d46ead3355750e90032feb34051a552a6f1c76cf1b4072d8d158af9de7"))

        assertThat(talkPageSeenDao.getTalkPageSeen("328b389f2063da236be9d363b272eb0fa6e065816607099c7db8c09e1c919617"), notNullValue())
        assertThat(talkPageSeenDao.getTalkPageSeen("foo"), nullValue())

        var allSeen = talkPageSeenDao.getAll()
        assertThat(allSeen.size, equalTo(2))

        talkPageSeenDao.deleteAll().blockingSubscribe()
        allSeen = talkPageSeenDao.getAll()
        assertThat(allSeen.size, equalTo(0))
    }

    @Test
    fun testNotification() = runBlocking {
        val rawJson = InstrumentationRegistry.getInstrumentation()
            .context.resources.assets.open("database/json/notifications.json")
            .bufferedReader()
            .use { it.readText() }

        val notifications = JsonUtil.decodeFromString<List<Notification>>(rawJson)!!

        notificationDao.insertNotifications(notifications)

        var enWikiList = notificationDao.getNotificationsByWiki(listOf("enwiki")).first()
        val zhWikiList = notificationDao.getNotificationsByWiki(listOf("zhwiki")).first()
        assertThat(enWikiList, notNullValue())
        assertThat(enWikiList.first().id, equalTo(123759827))
        assertThat(zhWikiList.first().id, equalTo(2470933))
        assertThat(enWikiList.first().isUnread, equalTo(false))
        assertThat(enWikiList.size, equalTo(2))
        assertThat(notificationDao.getAllNotifications().first().size, equalTo(3))

        val firstEnNotification = enWikiList.first()
        firstEnNotification.read = null
        notificationDao.updateNotification(firstEnNotification)

        // get updated item
        enWikiList = notificationDao.getNotificationsByWiki(listOf("enwiki")).first()
        assertThat(enWikiList.first().id, equalTo(123759827))
        assertThat(enWikiList.first().isUnread, equalTo(true))

        notificationDao.deleteNotification(firstEnNotification)
        assertThat(notificationDao.getAllNotifications().first().size, equalTo(2))
        assertThat(notificationDao.getNotificationsByWiki(listOf("enwiki")).first().size, equalTo(1))

        notificationDao.deleteNotification(notificationDao.getNotificationsByWiki(listOf("enwiki")).first().first())
        assertThat(notificationDao.getNotificationsByWiki(listOf("enwiki")).first().isEmpty(), equalTo(true))

        notificationDao.deleteNotification(notificationDao.getNotificationsByWiki(listOf("zhwiki")).first().first())
        assertThat(notificationDao.getAllNotifications().first().isEmpty(), equalTo(true))
    }
}
