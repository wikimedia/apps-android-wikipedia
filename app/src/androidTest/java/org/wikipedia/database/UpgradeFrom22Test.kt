package org.wikipedia.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.search.db.RecentSearchDao
import org.wikipedia.util.FileUtil
import org.wikipedia.util.log.L
import java.io.File
import java.util.*

@RunWith(AndroidJUnit4::class)
class UpgradeFrom22Test {
    private lateinit var db: AppDatabase
    private lateinit var recentSearchDao: RecentSearchDao
    private lateinit var readingListPageDao: ReadingListPageDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().context

        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName)

        val sql = FileUtil.readFile(context.assets.open("database/wikipedia_v22.sql"))


        var d = helper.createDatabase("wikipedia.db", 22)
        d.beginTransaction()
        d.execSQL(sql)
        d.setTransactionSuccessful()
        d.close()

        d = helper.runMigrationsAndValidate("wikipedia.db", 23, true, AppDatabase.MIGRATION_22_23)

        L.d(">>> " + d.isOpen)



        db = Room.databaseBuilder(context, AppDatabase::class.java, "wikipedia.db")
            //.createFromInputStream { context.assets.open("wikipedia_v22.db") }
            .createFromAsset("databases/wikipedia_v22.db")
            //.createFromFile(getRawFile("wikipedia_v22.db"))
            .build()
        recentSearchDao = db.recentSearchDao()
        readingListPageDao = db.readingListPageDao()

    }

    @After
    fun closeDb() {
        db.close()
    }
    
    @Test
    fun testReadAndWriteRecentSearch() {
        val now = Date()

        val recentSearches = recentSearchDao.getRecentSearches().blockingGet()

        assertThat(recentSearches.size, `is`(4))
        assertThat(recentSearches[0].text, `is`("Foo"))
        assertThat(recentSearches[1].text, `is`("Bar"))

    }
}
