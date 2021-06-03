package org.wikipedia.database.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [], version = 1)
abstract class WikipediaRoomDatabase : RoomDatabase() {
    //abstract fun userDao(): UserDao?

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
                        "wikipedia"
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