package org.wikipedia.database.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [History::class], version = 2)
abstract class WikipediaRoomDatabase : RoomDatabase() {
    abstract fun historyDAO(): HistoryDAO?

    companion object {
        @Volatile
        private var INSTANCE: WikipediaRoomDatabase? = null
        fun getAppRoomDatabase(context: Context): WikipediaRoomDatabase? {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        WikipediaRoomDatabase::class.java,
                        "wikipediaRoom.db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
