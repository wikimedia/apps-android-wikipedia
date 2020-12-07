package org.wikipedia.watchlist

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.DbUtil
import org.wikipedia.database.column.Column
import org.wikipedia.database.column.DateColumn
import org.wikipedia.database.column.LongColumn
import org.wikipedia.database.column.StrColumn
import org.wikipedia.database.contract.AppContentProviderContract

private const val TABLE = "localwatchlist"
private const val PATH = "localwatchlist"
private val URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH)

class WatchlistDatabaseTable : DatabaseTable<Watchlist?>(TABLE, URI) {

    override fun fromCursor(cursor: Cursor): Watchlist {
        return Watchlist(API_TITLE.`val`(cursor), DISPLAY_TITLE.`val`(cursor), TIMESTAMP.`val`(cursor), TIME_PERIOD.`val`(cursor))
    }

    override fun toContentValues(obj: Watchlist?): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(API_TITLE.name, obj!!.apiTitle)
        contentValues.put(DISPLAY_TITLE.name, obj.displayTitle)
        contentValues.put(TIMESTAMP.name, obj.timestamp.time)
        contentValues.put(TIME_PERIOD.name, obj.timePeriod)
        return contentValues
    }

    override fun getDBVersionIntroducedAt(): Int {
        return DB_VER_INTRODUCED
    }

    override fun getColumnsAdded(version: Int): Array<Column<*>> {
        return when (version) {
            DB_VER_INTRODUCED -> arrayOf(ID, API_TITLE, DISPLAY_TITLE, TIMESTAMP, TIME_PERIOD)
            else -> super.getColumnsAdded(version)
        }
    }

    override fun getPrimaryKeySelection(obj: Watchlist, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, SELECTION)
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: Watchlist): Array<String?> {
        return arrayOf(obj.apiTitle, obj.displayTitle)
    }

    // TODO: add method of getting time period for specific article.

    companion object {
        private const val DB_VER_INTRODUCED = 22
        private val ID = LongColumn(TABLE, BaseColumns._ID, "integer primary key")
        private val API_TITLE = StrColumn(TABLE, "apiTitle", "string")
        private val DISPLAY_TITLE = StrColumn(TABLE, "displayTitle", "string")
        private val TIMESTAMP = DateColumn(TABLE, "timestamp", "integer")
        private val TIME_PERIOD = LongColumn(TABLE, "timePeriod", "integer") // in days
        private val SELECTION = DbUtil.qualifiedNames(API_TITLE)
    }
}