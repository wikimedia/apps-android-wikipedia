package org.wikipedia.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wikipedia.WikipediaApp
import org.wikipedia.categories.db.Category
import org.wikipedia.categories.db.CategoryDao
import org.wikipedia.edit.db.EditSummary
import org.wikipedia.edit.db.EditSummaryDao
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.games.db.DailyGameHistoryDao
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.db.HistoryEntryDao
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.notifications.db.Notification
import org.wikipedia.notifications.db.NotificationDao
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.offline.db.OfflineObjectDao
import org.wikipedia.page.tabs.PageBackStackItem
import org.wikipedia.page.tabs.PageBackStackItemDao
import org.wikipedia.page.tabs.Tab
import org.wikipedia.page.tabs.TabDao
import org.wikipedia.pageimages.db.PageImage
import org.wikipedia.pageimages.db.PageImageDao
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.readinglist.database.RecommendedPage
import org.wikipedia.readinglist.db.ReadingListDao
import org.wikipedia.readinglist.db.ReadingListPageDao
import org.wikipedia.readinglist.db.RecommendedPageDao
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.search.db.RecentSearchDao
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.talk.db.TalkPageSeenDao
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.talk.db.TalkTemplateDao
import java.time.LocalDate

const val DATABASE_NAME = "wikipedia.db"
const val DATABASE_VERSION = 32

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
        Category::class,
        DailyGameHistory::class,
        RecommendedPage::class,
        Tab::class,
        PageBackStackItem::class
    ],
    version = DATABASE_VERSION
)
@TypeConverters(
    DateTypeConverter::class,
    WikiSiteTypeConverter::class,
    NamespaceTypeConverter::class,
    NotificationTypeConverters::class
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
    abstract fun categoryDao(): CategoryDao
    abstract fun dailyGameHistoryDao(): DailyGameHistoryDao
    abstract fun recommendedPageDao(): RecommendedPageDao
    abstract fun tabDao(): TabDao
    abstract fun pageBackStackItemDao(): PageBackStackItemDao

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
                } catch (_: Exception) {
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
                } catch (_: Exception) {
                    // ignore further errors
                }
                db.execSQL("ALTER TABLE TalkPageSeen_temp RENAME TO TalkPageSeen")

                // convert Edit Summaries table
                db.execSQL("CREATE TABLE IF NOT EXISTS `EditSummary` (`summary` TEXT NOT NULL, `lastUsed` INTEGER NOT NULL, PRIMARY KEY(`summary`))")
                try {
                    db.execSQL("INSERT OR REPLACE INTO EditSummary (summary, lastUsed) SELECT summary, lastUsed FROM editsummaries")
                } catch (_: Exception) {
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
                } catch (_: Exception) {
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
                } catch (_: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE IF EXISTS localreadinglist")
                db.execSQL("DROP TABLE IF EXISTS localreadinglistpage")

                // convert History table
                db.execSQL("CREATE TABLE IF NOT EXISTS `HistoryEntry` (`authority` TEXT NOT NULL, `lang` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `displayTitle` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namespace` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` INTEGER NOT NULL, `timeSpentSec` INTEGER NOT NULL)")
                try {
                    db.execSQL("INSERT INTO HistoryEntry (id, authority, lang, apiTitle, displayTitle, namespace, source, timestamp, timeSpentSec) SELECT _id, COALESCE(site,'$defaultAuthority'), COALESCE(lang,'$defaultLang'), COALESCE(title,'$defaultTitle'), COALESCE(displayTitle,''), COALESCE(namespace,''), COALESCE(source,2), COALESCE(timestamp,0), COALESCE(timeSpent,0) FROM history")
                } catch (_: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE IF EXISTS history")

                // convert Page Images table
                db.execSQL("CREATE TABLE IF NOT EXISTS `PageImage` (`lang` TEXT NOT NULL, `namespace` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `imageName` TEXT, PRIMARY KEY(`lang`, `namespace`, `apiTitle`))")
                try {
                    db.execSQL("INSERT OR REPLACE INTO PageImage (lang, namespace, apiTitle, imageName) SELECT COALESCE(lang,'$defaultLang'), COALESCE(namespace,''), COALESCE(title,'$defaultTitle'), imageName FROM pageimages")
                } catch (_: Exception) {
                    // ignore further errors
                }
                db.execSQL("DROP TABLE pageimages")
            }
        }
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `Notification` (`id` INTEGER NOT NULL, `wiki` TEXT NOT NULL, `read` TEXT, `category` TEXT NOT NULL, `type` TEXT NOT NULL, `revid` INTEGER NOT NULL, `title` TEXT, `agent` TEXT, `timestamp` TEXT, `contents` TEXT, PRIMARY KEY(`id`, `wiki`))")
            }
        }
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `TalkTemplate` (`id` INTEGER NOT NULL, `type` INTEGER NOT NULL, `order` INTEGER NOT NULL, `title` TEXT NOT NULL, `subject` TEXT NOT NULL, `message` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE HistoryEntry ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename the existing HistoryEntry table, which we're preserving for now (in case
                // things go wrong with migrations in the field).
                db.execSQL("ALTER TABLE HistoryEntry RENAME TO HistoryEntry_old")

                // Create the "new" HistoryEntry table, which will match the new HistoryEntry structure.
                db.execSQL("CREATE TABLE `HistoryEntry` (`authority` TEXT NOT NULL, `lang` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `displayTitle` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namespace` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` INTEGER NOT NULL, `prevId` INTEGER NOT NULL DEFAULT -1)")

                // Copy everything from the old table to the new one, minus the columns that were removed.
                db.execSQL("INSERT INTO HistoryEntry (authority, lang, apiTitle, displayTitle, id, namespace, timestamp, source) SELECT authority, lang, apiTitle, displayTitle, id, namespace, timestamp, source FROM HistoryEntry_old")

                // Add new columns to the PageImage table, will will now serve as a more general
                // table for page metadata, not just the thumbnail.
                db.execSQL("ALTER TABLE PageImage ADD COLUMN timeSpentSec INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE PageImage ADD COLUMN description TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE PageImage ADD COLUMN geoLat REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE PageImage ADD COLUMN geoLon REAL NOT NULL DEFAULT 0.0")

                // Copy the metadata from the removed columns in the old HistoryEntry table into the
                // new columns in the PageImage table.
                db.execSQL("UPDATE PageImage SET description = (SELECT description FROM HistoryEntry_old WHERE PageImage.lang = HistoryEntry_old.lang AND PageImage.namespace = HistoryEntry_old.namespace AND PageImage.apiTitle = HistoryEntry_old.apiTitle)")
                db.execSQL("UPDATE PageImage SET timeSpentSec = COALESCE((SELECT timeSpentSec FROM HistoryEntry_old WHERE PageImage.lang = HistoryEntry_old.lang AND PageImage.namespace = HistoryEntry_old.namespace AND PageImage.apiTitle = HistoryEntry_old.apiTitle), 0)")
            }
        }
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_HistoryEntry_lang_namespace_apiTitle ON HistoryEntry (lang, namespace, apiTitle)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_PageImage_lang_namespace_apiTitle ON PageImage (lang, namespace, apiTitle)")
            }
        }
        val MIGRATION_26_28 = object : Migration(26, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Rename the existing HistoryEntry table, which we're preserving for now (in case
                // things go wrong with migrations in the field).
                db.execSQL("ALTER TABLE HistoryEntry RENAME TO HistoryEntry_old")

                // Create the "new" HistoryEntry table, which will match the new HistoryEntry structure.
                db.execSQL("CREATE TABLE `HistoryEntry` (`authority` TEXT NOT NULL, `lang` TEXT NOT NULL, `apiTitle` TEXT NOT NULL, `displayTitle` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `namespace` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `source` INTEGER NOT NULL, `prevId` INTEGER NOT NULL DEFAULT -1)")

                // Create indexes on the new and old HistoryEntry table.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_HistoryEntry_lang_namespace_apiTitle ON HistoryEntry (lang, namespace, apiTitle)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_HistoryEntry_old_lang_namespace_apiTitle ON HistoryEntry_old (lang, namespace, apiTitle)")

                // Copy everything from the old table to the new one, minus the columns that were removed.
                db.execSQL("INSERT INTO HistoryEntry (authority, lang, apiTitle, displayTitle, id, namespace, timestamp, source) SELECT authority, lang, apiTitle, displayTitle, id, namespace, timestamp, source FROM HistoryEntry_old")

                // Add new columns to the PageImage table, will will now serve as a more general
                // table for page metadata, not just the thumbnail.
                db.execSQL("ALTER TABLE PageImage ADD COLUMN timeSpentSec INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE PageImage ADD COLUMN description TEXT DEFAULT ''")
                db.execSQL("ALTER TABLE PageImage ADD COLUMN geoLat REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE PageImage ADD COLUMN geoLon REAL NOT NULL DEFAULT 0.0")

                // Create an index on the PageImage table.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_PageImage_lang_namespace_apiTitle ON PageImage (lang, namespace, apiTitle)")

                // Copy the metadata from the removed columns in the old HistoryEntry table into the
                // new columns in the PageImage table, for PageImage rows that already exist.
                db.execSQL("UPDATE PageImage SET" +
                        " description = (SELECT HistoryEntry_old.description" +
                        "     FROM HistoryEntry_old" +
                        "     WHERE PageImage.lang = HistoryEntry_old.lang" +
                        "     AND PageImage.namespace = HistoryEntry_old.namespace" +
                        "     AND PageImage.apiTitle = HistoryEntry_old.apiTitle)," +
                        " timeSpentSec = COALESCE((SELECT HistoryEntry_old.timeSpentSec" +
                        "     FROM HistoryEntry_old" +
                        "     WHERE PageImage.lang = HistoryEntry_old.lang" +
                        "     AND PageImage.namespace = HistoryEntry_old.namespace" +
                        "     AND PageImage.apiTitle = HistoryEntry_old.apiTitle), 0)")

                // For PageImage rows that don't already exist (i.e. HistoryEntries that didn't have
                // a thumbnail), insert them and copy the other metadata.
                db.execSQL("INSERT INTO PageImage (lang, namespace, apiTitle, description, timeSpentSec)" +
                        " SELECT lang, namespace, apiTitle, description, COALESCE(timeSpentSec, 0) as timeSpentSec FROM" +
                        " (SELECT lang, namespace, apiTitle, description, MAX(COALESCE(timeSpentSec, 0)) as timeSpentSec" +
                        "     FROM HistoryEntry_old GROUP BY lang, namespace, apiTitle) AS HistoryUnique" +
                        " WHERE NOT EXISTS (SELECT 1 FROM PageImage" +
                        "     WHERE PageImage.lang = HistoryUnique.lang AND" +
                        "         PageImage.namespace = HistoryUnique.namespace AND" +
                        "         PageImage.apiTitle = HistoryUnique.apiTitle)")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS Category (" +
                        "title TEXT NOT NULL," +
                        "lang TEXT NOT NULL," +
                        "timeStamp INTEGER NOT NULL," +
                        "PRIMARY KEY (title, lang, timeStamp)" +
                        ")")
                db.execSQL("CREATE TABLE IF NOT EXISTS DailyGameHistory (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "    gameName INTEGER NOT NULL," +
                        "    language TEXT NOT NULL," +
                        "    year INTEGER NOT NULL," +
                        "    month INTEGER NOT NULL," +
                        "    day INTEGER NOT NULL," +
                        "    score INTEGER NOT NULL," +
                        "    playType INTEGER NOT NULL," +
                        "    gameData TEXT" +
                        ")")
            }
        }
        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS RecommendedPage (" +
                        "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        "    wiki TEXT NOT NULL," +
                        "    lang TEXT NOT NULL DEFAULT 'en'," +
                        "    namespace INTEGER NOT NULL," +
                        "    timestamp INTEGER NOT NULL," +
                        "    apiTitle TEXT NOT NULL," +
                        "    displayTitle TEXT NOT NULL," +
                        "    description TEXT," +
                        "    thumbUrl TEXT," +
                        "    status INTEGER NOT NULL DEFAULT 0" +
                        ")")
            }
        }
        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Step 1: Create a temporary table
                db.execSQL("CREATE TABLE Category_temp (" +
                        "    year INTEGER NOT NULL," +
                        "    month INTEGER NOT NULL," +
                        "    title TEXT NOT NULL," +
                        "    lang TEXT NOT NULL," +
                        "    count INTEGER NOT NULL," +
                        "    PRIMARY KEY (year, month, title, lang)" +
                        ")")

                // Step 2: Populate the new table with the transformed data from the old table
                db.execSQL("INSERT INTO Category_temp (year, month, title, lang, count)" +
                        "    SELECT" +
                        "        COALESCE(CAST(strftime('%Y', timeStamp / 1000, 'unixepoch') AS INTEGER), ${LocalDate.now().year}) AS year," +
                        "        COALESCE(CAST(strftime('%m', timeStamp / 1000, 'unixepoch') AS INTEGER), ${LocalDate.now().monthValue}) AS month," +
                        "        title," +
                        "        lang," +
                        "        COUNT(*) AS count" +
                        "    FROM Category GROUP BY year, month, title, lang")

                // Step 3: Drop the old table
                db.execSQL("DROP TABLE Category")
                // Step 4: Rename the temporary table to the original table name

                db.execSQL("ALTER TABLE Category_temp RENAME TO Category")
            }
        }
        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `Tab` (" +
                        "  id INTEGER KEY AUTOINCREMENT NOT NULL " +
                        ")")
                db.execSQL("CREATE TABLE IF NOT EXISTS `PageBackStackItem` (" +
                        "  id INTEGER KEY AUTOINCREMENT NOT NULL, " +
                        "  tabId INTEGER NOT NULL, " +
                        "  apiTitle TEXT NOT NULL, " +
                        "  displayTitle TEXT NOT NULL, " +
                        "  langCode TEXT NOT NULL, " +
                        "  namespace TEXT NOT NULL, " +
                        "  timestamp INTEGER NOT NULL, " +
                        "  scrollY INTEGER NOT NULL, " +
                        "  source INTEGER NOT NULL, " +
                        "  thumbUrl TEXT, " +
                        "  description TEXT, " +
                        "  extract TEXT " +
                        ")")
            }
        }

        val instance: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(WikipediaApp.instance, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23,
                    MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27,
                    MIGRATION_26_28, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30,
                    MIGRATION_30_31, MIGRATION_31_32)
                .allowMainThreadQueries() // TODO: remove after resolving main thread issues in DAO classes
                .fallbackToDestructiveMigration(false)
                .build()
        }
    }
}
