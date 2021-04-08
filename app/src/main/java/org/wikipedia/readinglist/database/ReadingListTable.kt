package org.wikipedia.readinglist.database

import android.content.ContentValues
import android.database.Cursor
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.column.Column
import org.wikipedia.database.contract.ReadingListContract
import java.util.*

class ReadingListTable : DatabaseTable<ReadingList>(ReadingListContract.TABLE, ReadingListContract.URI) {
    override fun fromCursor(cursor: Cursor): ReadingList {
        return ReadingList(ReadingListContract.Col.TITLE.value(cursor), ReadingListContract.Col.DESCRIPTION.value(cursor)).apply {
            id = ReadingListContract.Col.ID.value(cursor)
            atime = ReadingListContract.Col.ATIME.value(cursor)
            mtime = ReadingListContract.Col.MTIME.value(cursor)
            sizeBytes = ReadingListContract.Col.SIZEBYTES.value(cursor)
            dirty = ReadingListContract.Col.DIRTY.value(cursor) != 0
            remoteId = ReadingListContract.Col.REMOTEID.value(cursor)
        }
    }

    override fun getColumnsAdded(version: Int): Array<Column<*>> {
        return when (version) {
            dBVersionIntroducedAt -> arrayOf(
                    ReadingListContract.Col.ID,
                    ReadingListContract.Col.TITLE,
                    ReadingListContract.Col.MTIME,
                    ReadingListContract.Col.ATIME,
                    ReadingListContract.Col.DESCRIPTION,
                    ReadingListContract.Col.SIZEBYTES,
                    ReadingListContract.Col.DIRTY,
                    ReadingListContract.Col.REMOTEID
            )
            else -> super.getColumnsAdded(version)
        }
    }

    override fun toContentValues(obj: ReadingList): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(ReadingListContract.Col.TITLE.name, obj.dbTitle)
        contentValues.put(ReadingListContract.Col.MTIME.name, obj.mtime)
        contentValues.put(ReadingListContract.Col.ATIME.name, obj.atime)
        contentValues.put(ReadingListContract.Col.DESCRIPTION.name, obj.description)
        contentValues.put(ReadingListContract.Col.SIZEBYTES.name, obj.sizeBytes)
        contentValues.put(ReadingListContract.Col.DIRTY.name, if (obj.dirty) 1 else 0)
        contentValues.put(ReadingListContract.Col.REMOTEID.name, obj.remoteId)
        return contentValues
    }

    override fun getPrimaryKeySelection(obj: ReadingList, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, ReadingListContract.Col.SELECTION)
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: ReadingList): Array<String?> {
        return arrayOf(obj.dbTitle)
    }

    override val dBVersionIntroducedAt = 18
}
