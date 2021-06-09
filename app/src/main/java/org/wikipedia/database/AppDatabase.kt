package org.wikipedia.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.wikipedia.WikipediaApp
import org.wikipedia.edit.db.EditSummary
import org.wikipedia.edit.db.EditSummaryDao
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.offline.db.OfflineObjectDao
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.search.db.RecentSearchDao
import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.talk.db.TalkPageSeenDao

const val DATABASE_NAME = "wikipedia.db"
const val DATABASE_VERSION = 23

@Database(entities = [RecentSearch::class,
    TalkPageSeen::class,
    EditSummary::class,
    OfflineObject::class,
    ReadingList::class,
    ReadingListPage::class], version = DATABASE_VERSION)
@TypeConverters(DateTypeConverter::class,
    WikiSiteTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun talkPageSeenDao(): TalkPageSeenDao
    abstract fun editSummaryDao(): EditSummaryDao
    abstract fun offlineObjectDao(): OfflineObjectDao

    val readableDatabase: SupportSQLiteDatabase get() = openHelper.readableDatabase
    val writableDatabase: SupportSQLiteDatabase get() = openHelper.writableDatabase

    companion object {
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // convert Recent Searches table
                database.execSQL("CREATE TABLE recentsearches_temp (_id INTEGER NOT NULL, text TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(_id))")
                database.execSQL("INSERT INTO recentsearches_temp (_id, text, timestamp) SELECT _id, text, timestamp FROM recentsearches")
                database.execSQL("DROP TABLE recentsearches")
                database.execSQL("ALTER TABLE recentsearches_temp RENAME TO recentsearches")

                // convert Talk Pages Seen table
                database.execSQL("CREATE TABLE talkpageseen_temp (_id INTEGER NOT NULL, sha TEXT NOT NULL, PRIMARY KEY(_id))")
                database.execSQL("INSERT INTO talkpageseen_temp (_id, sha) SELECT _id, sha FROM talkpageseen")
                database.execSQL("DROP TABLE talkpageseen")
                database.execSQL("ALTER TABLE talkpageseen_temp RENAME TO talkpageseen")

                // convert Edit Summaries table
                database.execSQL("CREATE TABLE editsummaries_temp (_id INTEGER NOT NULL, summary TEXT NOT NULL, lastUsed INTEGER NOT NULL, PRIMARY KEY(_id))")
                database.execSQL("INSERT INTO editsummaries_temp (_id, summary, lastUsed) SELECT _id, summary, lastUsed FROM editsummaries")
                database.execSQL("DROP TABLE editsummaries")
                database.execSQL("ALTER TABLE editsummaries_temp RENAME TO editsummaries")

                // convert Offline Objects table
                database.execSQL("CREATE TABLE offlineobject_temp (_id INTEGER NOT NULL, url TEXT NOT NULL, lang TEXT NOT NULL, path TEXT NOT NULL, status INTEGER NOT NULL, usedby TEXT NOT NULL, PRIMARY KEY(_id))")
                database.execSQL("INSERT INTO offlineobject_temp (_id, url, lang, path, status, usedby) SELECT _id, url, lang, path, status, usedby FROM offlineobject")
                database.execSQL("DROP TABLE offlineobject")
                database.execSQL("ALTER TABLE offlineobject_temp RENAME TO offlineobject")

            }
        }

        private var INSTANCE: AppDatabase? = null

        fun getAppDatabase(): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(WikipediaApp.getInstance(), AppDatabase::class.java, DATABASE_NAME)
                        .addMigrations(MIGRATION_22_23)
                        .allowMainThreadQueries() // TODO: remove after migration
                        .fallbackToDestructiveMigration()
                        .openHelperFactory { configuration ->
                            FrameworkSQLiteOpenHelperFactory().create(
                                SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
                                    .callback(Database(configuration.callback))
                                    .name(configuration.name)
                                    .noBackupDirectory(configuration.useNoBackupDirectory)
                                    .build()
                            )
                        }
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}
