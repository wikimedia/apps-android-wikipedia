package org.wikipedia.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.wikipedia.history.db.HistoryEntryDao
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.offline.db.OfflineObjectDao
import org.wikipedia.pageimages.db.PageImageDao
import org.wikipedia.readinglist.db.ReadingListDao
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.search.db.RecentSearchDao
import org.wikipedia.talk.db.TalkPageSeenDao

@RunWith(Parameterized::class)
class UpgradeFromPreRoomTest(private val fromVersion: Int) {
    private lateinit var db: AppDatabase
    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var readingListDao: ReadingListDao
    private lateinit var readingListPageDao: ReadingListPageDao
    private lateinit var pageImageDao: PageImageDao
    private lateinit var historyDao: HistoryEntryDao
    private lateinit var historyWithImageDao: HistoryEntryWithImageDao
    private lateinit var offlineObjectDao: OfflineObjectDao
    private lateinit var talkPageSeenDao: TalkPageSeenDao

    @Before
    fun createDb() {
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java.canonicalName)

        var helperDb = helper.createDatabase(DB_NAME, fromVersion)
        InstrumentationRegistry.getInstrumentation().context.assets.open("database/wikipedia_v$fromVersion.sql").bufferedReader().lines().forEach {
            helperDb.execSQL(it)
        }
        helperDb.close()

        helperDb = helper.runMigrationsAndValidate(DB_NAME, 23, true,
            AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22, AppDatabase.MIGRATION_22_23)
        helperDb.close()

        db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22, AppDatabase.MIGRATION_22_23)
            .fallbackToDestructiveMigration()
            .build()
        recentSearchDao = db.recentSearchDao()
        readingListDao = db.readingListDao()
        readingListPageDao = db.readingListPageDao()
        historyDao = db.historyEntryDao()
        historyWithImageDao = db.historyEntryWithImageDao()
        pageImageDao = db.pageImagesDao()
        offlineObjectDao = db.offlineObjectDao()
        talkPageSeenDao = db.talkPageSeenDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testTablesAfterMigration() {
        val recentSearches = recentSearchDao.getRecentSearches().blockingGet()
        assertThat(recentSearches.size, equalTo(4))
        assertThat(recentSearches[0].text, equalTo("obama"))
        assertThat(recentSearches[3].text, equalTo("trump"))

        val readingLists = readingListDao.getAllLists()
        assertThat(readingLists.size, equalTo(3))
        assertThat(readingLists[0].pages.size, equalTo(2))

        assertThat(readingLists[0].pages[1].apiTitle, equalTo("Barack_Obama"))
        assertThat(readingLists[0].pages[1].displayTitle, equalTo("Barack Obama"))
        assertThat(readingLists[0].pages[1].thumbUrl, equalTo("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"))
        assertThat(readingLists[0].pages[1].lang, equalTo("en"))
        assertThat(readingLists[0].pages[1].description, equalTo("44th president of the United States"))
        assertThat(readingLists[0].pages[1].sizeBytes, equalTo(5695183))
        assertThat(readingLists[0].pages[1].remoteId, equalTo(44))

        assertThat(readingLists[1].pages[0].apiTitle, equalTo("Joe Biden"))
        assertThat(readingLists[1].pages[0].displayTitle, equalTo("Joe Biden"))
        assertThat(readingLists[1].pages[0].thumbUrl, nullValue())
        assertThat(readingLists[1].pages[0].lang, equalTo("en"))
        assertThat(readingLists[1].pages[0].description, nullValue())
        assertThat(readingLists[1].pages[0].remoteId, equalTo(43))

        assertThat(readingLists[1].title, equalTo("People"))
        assertThat(readingLists[1].description, equalTo(""))
        assertThat(readingLists[1].remoteId, equalTo(101))
        assertThat(readingLists[1].pages.size, equalTo(3))

        assertThat(readingLists[2].title, equalTo("More people"))
        assertThat(readingLists[2].description, equalTo("Example list description"))
        assertThat(readingLists[2].pages.size, equalTo(1))
        assertThat(readingLists[2].pages[0].apiTitle, equalTo("ברק_אובמה"))

        val pageImages = pageImageDao.getAllPageImages()
        assertThat(pageImages.size, equalTo(7))
        assertThat(pageImages[0].apiTitle, equalTo("Barack_Obama"))
        assertThat(pageImages[0].imageName, equalTo("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"))

        val historyEntries = historyWithImageDao.findEntriesBySearchTerm("%%")
        assertThat(historyEntries.size, equalTo(6))
        assertThat(historyEntries[0].apiTitle, equalTo("ברק_אובמה"))
        assertThat(historyEntries[0].displayTitle, equalTo("ברק אובמה"))
        assertThat(historyEntries[0].lang, equalTo("he"))
        assertThat(historyEntries[0].imageName, equalTo("https://upload.wikimedia.org/wikipedia/commons/thumb/8/8d/President_Barack_Obama.jpg/256px-President_Barack_Obama.jpg"))
        assertThat(historyEntries[4].apiTitle, equalTo("Joe_Biden"))
        assertThat(historyEntries[4].lang, equalTo("en"))

        val historyEntry = historyDao.findEntryBy("ru.wikipedia.org", "ru", "Обама,_Барак")!!
        assertThat(historyEntry.displayTitle, equalTo("Обама, Барак"))

        if (fromVersion == 22) {
            assertThat(talkPageSeenDao.getAll().size, equalTo(2))
            assertThat(offlineObjectDao.getOfflineObject("https://en.wikipedia.org/api/rest_v1/page/summary/Joe_Biden")!!.path, equalTo("/data/user/0/org.wikipedia.dev/files/offline_files/481b1ef996728fd9994bd97ab19733d8"))
        } else {
            assertThat(talkPageSeenDao.getAll().size, equalTo(0))
            assertThat(offlineObjectDao.getOfflineObject("https://en.wikipedia.org/api/rest_v1/page/summary/Joe_Biden"), nullValue())
        }
    }

    companion object {
        const val DB_NAME = "wikipedia.db"

        @JvmStatic
        @Parameterized.Parameters
        fun params(): List<Any> {
            return listOf(19, 22)
        }
    }
}
