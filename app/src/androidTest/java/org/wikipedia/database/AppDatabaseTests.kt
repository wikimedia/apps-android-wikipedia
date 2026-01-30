package org.wikipedia.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
import org.wikipedia.util.log.L
import java.util.Date

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
    fun testRecentSearch() = runBlocking {
        val now = Date()

        recentSearchDao.insertRecentSearch(RecentSearch("Foo"))
        recentSearchDao.insertRecentSearch(RecentSearch("Bar", now))

        var results = recentSearchDao.getRecentSearches()
        assertEquals(2, results.size)
        assertEquals("Foo", results[0].text)
        assertEquals("Bar", results[1].text)
        assertEquals(now, results[1].timestamp)

        recentSearchDao.insertRecentSearch(RecentSearch("Baz"))
        results = recentSearchDao.getRecentSearches()
        assertEquals(3, results.size)

        recentSearchDao.deleteAll()
        results = recentSearchDao.getRecentSearches()
        assertEquals(0, results.size)
    }

    @Test
    fun testTalkPageSeen() {
        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, msg ->
            run {
                L.e(msg)
            }
        }) { withContext(Dispatchers.Main) {
            talkPageSeenDao.insertTalkPageSeen(TalkPageSeen("328b389f2063da236be9d363b272eb0fa6e065816607099c7db8c09e1c919617"))
            talkPageSeenDao.insertTalkPageSeen(TalkPageSeen("5fbbb2d46ead3355750e90032feb34051a552a6f1c76cf1b4072d8d158af9de7"))
            assertNotNull(talkPageSeenDao.getTalkPageSeen("328b389f2063da236be9d363b272eb0fa6e065816607099c7db8c09e1c919617"))
            assertNull(talkPageSeenDao.getTalkPageSeen("foo"))

            var allSeen = talkPageSeenDao.getAll()
            assertEquals(2, allSeen.count())

            talkPageSeenDao.deleteAll()
            allSeen = talkPageSeenDao.getAll()
            assertEquals(0, allSeen.count())
        }
        }
    }

    @Test
    fun testNotification() = runBlocking {
        val rawJson = InstrumentationRegistry.getInstrumentation()
            .context.resources.assets.open("database/json/notifications.json")
            .bufferedReader()
            .use { it.readText() }

        val notifications = JsonUtil.decodeFromString<List<Notification>>(rawJson)!!

        notificationDao.insertNotifications(notifications)

        var enWikiList = notificationDao.getNotificationsByWiki(listOf("enwiki"))
        val zhWikiList = notificationDao.getNotificationsByWiki(listOf("zhwiki"))
        assertNotNull(enWikiList)
        assertEquals(123759827, enWikiList.first().id)
        assertEquals(2470933, zhWikiList.first().id)
        assertEquals(false, enWikiList.first().isUnread)
        assertEquals(2, enWikiList.size)
        assertEquals(3, notificationDao.getAllNotifications().size)

        val firstEnNotification = enWikiList.first()
        firstEnNotification.read = null
        notificationDao.updateNotification(firstEnNotification)

        // get updated item
        enWikiList = notificationDao.getNotificationsByWiki(listOf("enwiki"))
        assertEquals(123759827, enWikiList.first().id)
        assertEquals(true, enWikiList.first().isUnread)

        notificationDao.deleteNotification(firstEnNotification)
        assertEquals(2, notificationDao.getAllNotifications().size)
        assertEquals(1, notificationDao.getNotificationsByWiki(listOf("enwiki")).size)

        notificationDao.deleteNotification(notificationDao.getNotificationsByWiki(listOf("enwiki")).first())
        assertTrue(notificationDao.getNotificationsByWiki(listOf("enwiki")).isEmpty())

        notificationDao.deleteNotification(notificationDao.getNotificationsByWiki(listOf("zhwiki")).first())
        assertTrue(notificationDao.getAllNotifications().isEmpty())
    }
}
