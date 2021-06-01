package org.wikipedia.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wikipedia.WikipediaApp
import org.wikipedia.search.RecentSearch
import org.wikipedia.search.RecentSearchDao
import org.wikipedia.talk.TalkPageSeen
import org.wikipedia.talk.TalkPageSeenDao

@Database(entities = [RecentSearch::class, TalkPageSeen::class], version = 24)
@TypeConverters(DateTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun talkPageSeenDao(): TalkPageSeenDao

    companion object {
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE recentsearches_temp (_id INTEGER NOT NULL, text TEXT NOT NULL, timestamp INTEGER NOT NULL, PRIMARY KEY(_id))")
                database.execSQL("INSERT INTO recentsearches_temp (_id, text, timestamp) SELECT _id, text, timestamp FROM recentsearches")
                database.execSQL("DROP TABLE recentsearches")
                database.execSQL("ALTER TABLE recentsearches_temp RENAME TO recentsearches")
            }
        }
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        private var INSTANCE: AppDatabase? = null

        fun getAppDatabase(): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(WikipediaApp.getInstance(), AppDatabase::class.java, "wikipedia.db")
                        .addMigrations(MIGRATION_22_23, MIGRATION_23_24)
                        .allowMainThreadQueries() // TODO: remove after migration
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}
