package org.wikipedia.search

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.column.Column
import org.wikipedia.database.contract.SearchHistoryContract

class RecentSearchDatabaseTable : DatabaseTable<RecentSearch>(SearchHistoryContract.TABLE, SearchHistoryContract.Query.URI) {
    override fun fromCursor(cursor: Cursor): RecentSearch {
        val title = SearchHistoryContract.Col.TEXT.value(cursor)
        val timestamp = SearchHistoryContract.Col.TIMESTAMP.value(cursor)
        return RecentSearch(title, timestamp)
    }

    override fun getColumnsAdded(version: Int): Array<Column<*>> {
        return when (version) {
            DB_VER_INTRODUCED -> arrayOf(SearchHistoryContract.Col.ID, SearchHistoryContract.Col.TEXT, SearchHistoryContract.Col.TIMESTAMP)
            else -> super.getColumnsAdded(version)
        }
    }

    override val dBVersionIntroducedAt: Int
        get() = DB_VER_INTRODUCED

    override fun toContentValues(obj: RecentSearch): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(SearchHistoryContract.Col.TEXT.name, obj.text)
        contentValues.put(SearchHistoryContract.Col.TIMESTAMP.name, obj.timestamp.time)
        return contentValues
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: RecentSearch): Array<String?> {
        return arrayOf(obj.text)
    }

    override fun getPrimaryKeySelection(obj: RecentSearch, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, SearchHistoryContract.Col.SELECTION)
    }

    override fun onUpgradeSchema(db: SQLiteDatabase, fromVersion: Int, toVersion: Int) {
        if (toVersion == DB_VER_FIXED_FROM_CORRUPTION) {
            createTables(db)
        }
    }

    companion object {
        const val DB_VER_INTRODUCED = 5
        const val DB_VER_FIXED_FROM_CORRUPTION = 22
    }
}
