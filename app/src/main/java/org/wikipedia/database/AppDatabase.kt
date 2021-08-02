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
import org.wikipedia.staticdata.MainPageNameData
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

    companion object {
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // introduced Offline Object table
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // introduced Talk Page Seen table
            }
        }
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val defaultLang = WikipediaApp.getInstance().appOrSystemLanguageCode
                val defaultAuthority = WikipediaApp.getInstance().wikiSite.authority()
                val defaultTitle = MainPageNameData.valueFor(defaultLang)

                // convert Recent Searches table
                database.execSQL("CREATE TABLE IF NOT EXISTS `RecentSearch` (`text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`text`))")
                database.execSQL("INSERT OR REPLACE INTO RecentSearch (text, timestamp) SELECT text, timestamp FROM recentsearches")
                database.execSQL("DROP TABLE recentsearches")

                // convert Talk Pages Seen table
                database.execSQL("CREATE TABLE IF NOT EXISTS `TalkPageSeen_temp` (`sha` TEXT NOT NULL, PRIMARY KEY(`sha`))")
                database.query("SELECT * FROM sqlite_master WHERE type='table' AND name='talkpageseen'").use {
                    if (it.count > 0) {
                        database.execSQL("INSERT OR REPLACE INTO TalkPageSeen_temp (sha) SELECT sha FROM talkpageseen")
                        database.execSQL("DROP TABLE talkpageseen")
                    }
                }
                database.execSQL("ALTER TABLE TalkPageSeen_temp RENAME TO TalkPageSeen")

                // convert Edit Summaries table
                database.execSQL("CREATE TABLE IF NOT EXISTS `EditSummary` (`summary` TEXT NOT NULL, `lastUsed` INTEGER NOT NULL, PRIMARY KEY(`summary`))")
                database.execSQL("INSERT OR REPLACE INTO EditSummary (summary, lastUsed) SELECT summary, lastUsed FROM editsummaries")
                database.execSQL("DROP TABLE editsummaries")

                // convert Offline Objects table
                database.execSQL("CREATE TABLE IF NOT EXISTS `OfflineObject_temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `lang` TEXT NOT NULL, `path` TEXT NOT NULL, `status` INTEGER NOT NULL, `usedByStr` TEXT NOT NULL)")
                database.query("SELECT * FROM sqlite_master WHERE type='table' AND name='offlineobject'").use {
                    if (it.count > 0) {
                        database.execSQL("INSERT INTO OfflineObject_temp (id, url, lang, path, status, usedByStr) SELECT _id, url, lang, path, status, usedby FROM offlineobject")
                        database.execSQL("DROP TABLE offlineobject")
                    }
                }
                database.execSQL("ALTER TABLE OfflineObject_temp RENAME TO OfflineObject")

                // convert Reading List table
                database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingList` (`listTitle` TEXT NOT NULL, `description` TEXT, `mtime` INTEGER NOT NULL, `atime` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sizeBytes` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, `remoteId` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO ReadingList (id, listTitle, description, mtime, atime, sizeBytes, dirty, remoteId) SELECT _id, COALESCE(readingListTitle,''), readingListDescription, readingListMtime, readingListAtime, readingListSizeBytes, readingListDirty, readingListRemoteId FROM localreadinglist")
                database.execSQL("DROP TABLE localreadinglist")

                // convert Reading List Page table
                database.execSQL("CREATE TABLE IF NOT EXISTS `ReadingListPage` (`wiki` TEXT NOT NULL, `namespace` INTEGER NOT NULL, `displayTitle` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `description` TEXT, `thumbUrl` TEXT, `listId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mtime` INTEGER NOT NULL, `atime` INTEGER NOT NULL, `offline` INTEGER NOT NULL, `status` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, `lang` TEXT NOT NULL, `revId` INTEGER NOT NULL, `remoteId` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO ReadingListPage (id, wiki, namespace, displayTitle, apiTitle, description, thumbUrl, listId, mtime, atime, offline, status, sizeBytes, lang, revId, remoteId) SELECT _id, site, namespace, title, COALESCE(apiTitle,title), description, thumbnailUrl, listId, mtime, atime, offline, status, sizeBytes, lang, revId, remoteId FROM localreadinglistpage")
                database.execSQL("DROP TABLE localreadinglistpage")

                // convert History table
                database.execSQL("CREATE TABLE IF NOT EXISTS `HistoryEntry` (`authority` TEXT NOT NULL, `lang` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `displayTitle` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namespace` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` INTEGER NOT NULL, `timeSpentSec` INTEGER NOT NULL)")
                database.execSQL("INSERT INTO HistoryEntry (id, authority, lang, apiTitle, displayTitle, namespace, source, timestamp, timeSpentSec) SELECT _id, COALESCE(site,'$defaultAuthority'), COALESCE(lang,'$defaultLang'), COALESCE(title,'$defaultTitle'), COALESCE(displayTitle,''), COALESCE(namespace,''), COALESCE(source,2), COALESCE(timestamp,0), COALESCE(timeSpent,0) FROM history")
                database.execSQL("DROP TABLE history")

                // convert Page Images table
                database.execSQL("CREATE TABLE IF NOT EXISTS `PageImage` (`lang` TEXT NOT NULL, `namespace` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `imageName` TEXT, PRIMARY KEY(`lang`, `namespace`, `apiTitle`))")
                database.execSQL("INSERT OR REPLACE INTO PageImage (lang, namespace, apiTitle, imageName) SELECT COALESCE(lang,'$defaultLang'), COALESCE(namespace,''), COALESCE(title,'$defaultTitle'), imageName FROM pageimages")
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
                        .addMigrations(MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23)
                        .allowMainThreadQueries() // TODO: remove after migration
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return instance!!
        }
    }
}
