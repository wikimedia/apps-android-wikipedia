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
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.offline.db.OfflineObjectDao
import org.wikipedia.page.tabs.Tab
import org.wikipedia.page.tabs.TabDao
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
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.talk.db.TalkTemplateDao

const val DATABASE_NAME = "wikipedia.db"
const val DATABASE_VERSION = 27

@Database(
    entities = [
        HistoryEntry::class,
        PageImage::class,
        RecentSearch::class,
        TalkPageSeen::class,
        EditSummary::class,
        OfflineObject::class,
        ReadingList::class,
        ReadingListPage::class,
        Notification::class,
        TalkTemplate::class,
        Tab::class
    ],
    version = DATABASE_VERSION
)
@TypeConverters(
    DateTypeConverter::class,
    WikiSiteTypeConverter::class,
    NamespaceTypeConverter::class,
    NotificationTypeConverters::class,
    PageBackStackItemTypeConverter::class
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
    abstract fun notificationDao(): NotificationDao
    abstract fun talkTemplateDao(): TalkTemplateDao
    abstract fun tabDao(): TabDao

    companion object {
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // introduced Offline Object table
            }
        }
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // introduced Talk Page Seen table
            }
        }
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val defaultLang = WikipediaApp.instance.appOrSystemLanguageCode
                val defaultAuthority = WikipediaApp.instance.wikiSite.authority()
                val defaultTitle = MainPageNameData.valueFor(defaultLang)

                // convert Recent Searches table
                db.execSQL("CREATE TABLE IF NOT EXISTS `RecentSearch` (`text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`text`))")
                try {
                    db.execSQL("INSERT OR REPLACE INTO RecentSearch (text, timestamp) SELECT text, timestamp FROM recentsearches")
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE IF EXISTS recentsearches")

                // convert Talk Pages Seen table
                db.execSQL("CREATE TABLE IF NOT EXISTS `TalkPageSeen_temp` (`sha` TEXT NOT NULL, PRIMARY KEY(`sha`))")
                try {
                    db.query("SELECT * FROM sqlite_master WHERE type='table' AND name='talkpageseen'").use {
                        if (it.count > 0) {
                            db.execSQL("INSERT OR REPLACE INTO TalkPageSeen_temp (sha) SELECT sha FROM talkpageseen")
                            db.execSQL("DROP TABLE talkpageseen")
                        }
                    }
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("ALTER TABLE TalkPageSeen_temp RENAME TO TalkPageSeen")

                // convert Edit Summaries table
                db.execSQL("CREATE TABLE IF NOT EXISTS `EditSummary` (`summary` TEXT NOT NULL, `lastUsed` INTEGER NOT NULL, PRIMARY KEY(`summary`))")
                try {
                    db.execSQL("INSERT OR REPLACE INTO EditSummary (summary, lastUsed) SELECT summary, lastUsed FROM editsummaries")
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE IF EXISTS editsummaries")

                // convert Offline Objects table
                db.execSQL("CREATE TABLE IF NOT EXISTS `OfflineObject_temp` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `lang` TEXT NOT NULL, `path` TEXT NOT NULL, `status` INTEGER NOT NULL, `usedByStr` TEXT NOT NULL)")
                try {
                    db.query("SELECT * FROM sqlite_master WHERE type='table' AND name='offlineobject'").use {
                        if (it.count > 0) {
                            db.execSQL("INSERT INTO OfflineObject_temp (id, url, lang, path, status, usedByStr) SELECT _id, url, lang, path, status, usedby FROM offlineobject")
                            db.execSQL("DROP TABLE offlineobject")
                        }
                    }
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("ALTER TABLE OfflineObject_temp RENAME TO OfflineObject")

                // Delete vestigial Reading List tables that might have been left over from very old DB versions.
                db.execSQL("DROP TABLE IF EXISTS readinglist")
                db.execSQL("DROP TABLE IF EXISTS readinglistpage")

                // convert Reading List tables
                db.execSQL("CREATE TABLE IF NOT EXISTS `ReadingList` (`listTitle` TEXT NOT NULL, `description` TEXT, `mtime` INTEGER NOT NULL, `atime` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sizeBytes` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, `remoteId` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `ReadingListPage` (`wiki` TEXT NOT NULL, `namespace` INTEGER NOT NULL, `displayTitle` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `description` TEXT, `thumbUrl` TEXT, `listId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mtime` INTEGER NOT NULL, `atime` INTEGER NOT NULL, `offline` INTEGER NOT NULL, `status` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, `lang` TEXT NOT NULL, `revId` INTEGER NOT NULL, `remoteId` INTEGER NOT NULL)")
                try {
                    db.execSQL("INSERT INTO ReadingList (id, listTitle, description, mtime, atime, sizeBytes, dirty, remoteId) SELECT _id, COALESCE(readingListTitle,''), readingListDescription, readingListMtime, readingListAtime, readingListSizeBytes, readingListDirty, readingListRemoteId FROM localreadinglist")
                    db.execSQL("INSERT INTO ReadingListPage (id, wiki, namespace, displayTitle, apiTitle, description, thumbUrl, listId, mtime, atime, offline, status, sizeBytes, lang, revId, remoteId) SELECT _id, site, namespace, title, COALESCE(apiTitle,title), description, thumbnailUrl, listId, mtime, atime, offline, status, sizeBytes, lang, revId, remoteId FROM localreadinglistpage")
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE IF EXISTS localreadinglist")
                db.execSQL("DROP TABLE IF EXISTS localreadinglistpage")

                // convert History table
                db.execSQL("CREATE TABLE IF NOT EXISTS `HistoryEntry` (`authority` TEXT NOT NULL, `lang` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `displayTitle` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namespace` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` INTEGER NOT NULL, `timeSpentSec` INTEGER NOT NULL)")
                try {
                    db.execSQL("INSERT INTO HistoryEntry (id, authority, lang, apiTitle, displayTitle, namespace, source, timestamp, timeSpentSec) SELECT _id, COALESCE(site,'$defaultAuthority'), COALESCE(lang,'$defaultLang'), COALESCE(title,'$defaultTitle'), COALESCE(displayTitle,''), COALESCE(namespace,''), COALESCE(source,2), COALESCE(timestamp,0), COALESCE(timeSpent,0) FROM history")
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE IF EXISTS history")

                // convert Page Images table
                db.execSQL("CREATE TABLE IF NOT EXISTS `PageImage` (`lang` TEXT NOT NULL, `namespace` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `imageName` TEXT, PRIMARY KEY(`lang`, `namespace`, `apiTitle`))")
                try {
                    db.execSQL("INSERT OR REPLACE INTO PageImage (lang, namespace, apiTitle, imageName) SELECT COALESCE(lang,'$defaultLang'), COALESCE(namespace,''), COALESCE(title,'$defaultTitle'), imageName FROM pageimages")
                } catch (e: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE pageimages")
            }
        }
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `Notification` (`id` INTEGER NOT NULL, `wiki` TEXT NOT NULL, `read` TEXT, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `revid` INTEGER NOT NULL, `title` TEXT, `agent` TEXT, `timestamp` TEXT, `contents` TEXT, PRIMARY KEY(`id`, `wiki`))")
            }
        }
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `TalkTemplate` (`id` INTEGER NOT NULL, `type` INTEGER NOT NULL, `order` INTEGER NOT NULL, `title` TEXT NOT NULL, `subject` TEXT NOT NULL, `message` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE HistoryEntry ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `Tab` (`id` INTEGER NOT NULL, `backStack` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val instance: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(WikipediaApp.instance, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27
                )
                .allowMainThreadQueries() // TODO: remove after migration
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
