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

@Database(entities = [RecentSearch::class], version = 23)
@TypeConverters(DateTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {}
        }

        private var INSTANCE: AppDatabase? = null

        fun getAppDatabase(): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class) {
                    INSTANCE = Room.databaseBuilder(WikipediaApp.getInstance(), AppDatabase::class.java, "wikipedia")
                        .addMigrations(MIGRATION_22_23)
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE!!
        }
    }
}
