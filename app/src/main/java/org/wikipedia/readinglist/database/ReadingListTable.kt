package org.wikipedia.readinglist.database

import android.database.Cursor
import androidx.core.content.contentValuesOf
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.column.Column
import org.wikipedia.database.contract.ReadingListContract

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

    public override fun toContentValues(obj: ReadingList) = contentValuesOf(
            ReadingListContract.Col.TITLE.name to obj.dbTitle,
            ReadingListContract.Col.MTIME.name to obj.mtime,
            ReadingListContract.Col.ATIME.name to obj.atime,
            ReadingListContract.Col.DESCRIPTION.name to obj.description,
            ReadingListContract.Col.SIZEBYTES.name to obj.sizeBytesFromPages,
            ReadingListContract.Col.DIRTY.name to if (obj.dirty) 1 else 0,
            ReadingListContract.Col.REMOTEID.name to obj.remoteId
    )

    override fun getPrimaryKeySelection(obj: ReadingList, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, ReadingListContract.Col.SELECTION)
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: ReadingList): Array<String?> {
        return arrayOf(obj.dbTitle)
    }

    override val dBVersionIntroducedAt = 18
}
