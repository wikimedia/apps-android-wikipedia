package org.wikipedia.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
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
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java.canonicalName)

        var d1 = helper.createDatabase(DB_NAME, 22)
        InstrumentationRegistry.getInstrumentation().context.assets.open("database/wikipedia_v22.sql").bufferedReader().lines().forEach {
            d1.execSQL(it)
        }
        d1.close()
        L.d(">>> " + d1.isOpen)

        var d2 = helper.runMigrationsAndValidate(DB_NAME, 23, true, AppDatabase.MIGRATION_22_23)
        d2.close()

        L.d(">>> " + d2.isOpen)



        db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_22_23)
            .build()
        recentSearchDao = db.recentSearchDao()
        readingListPageDao = db.readingListPageDao()



        val recentSearches = recentSearchDao.getRecentSearches().blockingGet()
        L.d(">>> " + recentSearches.size)

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

    companion object {
        const val DB_NAME = "wikipedia.db"
    }
}
