package org.wikipedia.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.search.db.RecentSearchDao
import java.util.*

@RunWith(AndroidJUnit4::class)
class AppDatabaseTests {
    private lateinit var db: AppDatabase
    private lateinit var recentSearchDao: RecentSearchDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        recentSearchDao = db.recentSearchDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testReadAndWriteRecentSearch() {
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
}
