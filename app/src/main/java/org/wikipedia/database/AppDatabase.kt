package org.wikipedia.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wikipedia.WikipediaApp
import org.wikipedia.edit.db.EditSummary
import org.wikipedia.edit.db.EditSummaryDao
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.db.HistoryEntryDao
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.offline.db.OfflineObjectDao
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.pageimages.db.PageImageDao
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.db.ReadingListDao
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.search.db.RecentSearchDao
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.talk.db.TalkPageSeenDao

const val DATABASE_NAME = "wikipedia.db"
const val DATABASE_VERSION = 23

@Database(
    entities = [
        HistoryEntry::class,
        PageImage::class,
        RecentSearch::class,
        TalkPageSeen::class,
        EditSummary::class,
        OfflineObject::class,
        ReadingList::class,
        ReadingListPage::class
    ],
    version = DATABASE_VERSION
)
@TypeConverters(
    DateTypeConverter::class,
    WikiSiteTypeConverter::class,
    NamespaceTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyEntryDao(): HistoryEntryDao
    abstract fun historyEntryWithImageDao(): HistoryEntryWithImageDao
    abstract fun pageImagesDao(): PageImageDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun talkPageSeenDao(): TalkPageSeenDao
    abstract fun editSummaryDao(): EditSummaryDao
    abstract fun offlineObjectDao(): OfflineObjectDao
    abstract fun readingListDao(): ReadingListDao
    abstract fun readingListPageDao(): ReadingListPageDao

    val readableDatabase: SupportSQLiteDatabase get() = openHelper.readableDatabase
    val writableDatabase: SupportSQLiteDatabase get() = openHelper.writableDatabase

    companion object {
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // convert Recent Searches table
                database.execSQL("CREATE TABLE RecentSearch (text TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(text))")
                database.execSQL("INSERT INTO RecentSearch (text, timestamp) SELECT text, timestamp FROM recentsearches")
                database.execSQL("DROP TABLE recentsearches")

                // convert Talk Pages Seen table
                database.execSQL("CREATE TABLE TalkPageSeen_temp (sha TEXT NOT NULL, PRIMARY KEY(sha))")
                database.execSQL("INSERT INTO TalkPageSeen_temp (sha) SELECT sha FROM talkpageseen")
                database.execSQL("DROP TABLE talkpageseen")
                database.execSQL("ALTER TABLE TalkPageSeen_temp RENAME TO TalkPageSeen")

                // convert Edit Summaries table
                database.execSQL("CREATE TABLE EditSummary (summary TEXT NOT NULL, lastUsed INTEGER NOT NULL, PRIMARY KEY(summary))")
                database.execSQL("INSERT INTO EditSummary (summary, lastUsed) SELECT summary, lastUsed FROM editsummaries")
                database.execSQL("DROP TABLE editsummaries")

                // convert Offline Objects table
                database.execSQL("CREATE TABLE OfflineObject_temp (id INTEGER NOT NULL, url TEXT NOT NULL, lang TEXT NOT NULL, path TEXT NOT NULL, status INTEGER NOT NULL, usedByStr TEXT NOT NULL, PRIMARY KEY(id))")
                database.execSQL("INSERT INTO OfflineObject_temp (id, url, lang, path, status, usedByStr) SELECT _id, url, lang, path, status, usedby FROM offlineobject")
                database.execSQL("DROP TABLE offlineobject")
                database.execSQL("ALTER TABLE OfflineObject_temp RENAME TO OfflineObject")

                // convert Reading List table
                database.execSQL("CREATE TABLE ReadingList (id INTEGER NOT NULL, listTitle TEXT NOT NULL, description TEXT, mtime INTEGER NOT NULL, atime INTEGER NOT NULL, sizeBytes INTEGER NOT NULL, dirty INTEGER NOT NULL, remoteId INTEGER NOT NULL, PRIMARY KEY(id))")
                database.execSQL("INSERT INTO ReadingList (id, listTitle, description, mtime, atime, sizeBytes, dirty, remoteId) SELECT _id, readingListTitle, readingListDescription, readingListMtime, readingListAtime, readingListSizeBytes, readingListDirty, readingListRemoteId FROM localreadinglist")
                database.execSQL("DROP TABLE localreadinglist")

                // convert Reading List Page table
                database.execSQL("CREATE TABLE ReadingListPage (id INTEGER NOT NULL, wiki TEXT NOT NULL, namespace INTEGER NOT NULL, displayTitle TEXT NOT NULL, apiTitle TEXT NOT NULL, description TEXT, thumbUrl TEXT, listId INTEGER NOT NULL, mtime INTEGER NOT NULL, atime INTEGER NOT NULL, offline INTEGER NOT NULL, status INTEGER NOT NULL, sizeBytes INTEGER NOT NULL, lang TEXT NOT NULL, revId INTEGER NOT NULL, remoteId INTEGER NOT NULL, PRIMARY KEY(id))")
                database.execSQL("INSERT INTO ReadingListPage (id, wiki, namespace, displayTitle, apiTitle, description, thumbUrl, listId, mtime, atime, offline, status, sizeBytes, lang, revId, remoteId) SELECT _id, site, namespace, title, apiTitle, description, thumbnailUrl, listId, mtime, atime, offline, status, sizeBytes, lang, revId, remoteId FROM localreadinglistpage")
                database.execSQL("DROP TABLE localreadinglistpage")

                // convert History table
                database.execSQL("CREATE TABLE IF NOT EXISTS `HistoryEntry` (`authority` TEXT NOT NULL, `lang` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `displayTitle` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namespace` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` INTEGER NOT NULL, `timeSpentSec` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO HistoryEntry (id, authority, lang, apiTitle, displayTitle, namespace, source, timestamp, timeSpentSec) SELECT _id, site, lang, title, displayTitle, COALESCE(namespace,''), source, timestamp, timeSpent FROM history")
                database.execSQL("DROP TABLE history")

                // convert Page Images table
                database.execSQL("CREATE TABLE IF NOT EXISTS `PageImage` (`lang` TEXT NOT NULL, `namespace` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `imageName` TEXT, PRIMARY KEY(`lang`, `namespace`, `apiTitle`))")
                database.execSQL("INSERT INTO PageImage (lang, namespace, apiTitle, imageName) SELECT lang, COALESCE(namespace,''), title, imageName FROM pageimages")
                database.execSQL("DROP TABLE pageimages")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getAppDatabase(): AppDatabase {
            if (instance == null) {
                synchronized(AppDatabase::class) {
                    instance = Room.databaseBuilder(
                        WikipediaApp.getInstance(),
                        AppDatabase::class.java,
                        DATABASE_NAME
                    )
                        .addMigrations(MIGRATION_22_23)
                        .allowMainThreadQueries() // TODO: remove after migration
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return instance!!
        }
    }
}
