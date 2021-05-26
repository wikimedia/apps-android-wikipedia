package org.wikipedia.readinglist.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.column.Column
import org.wikipedia.database.contract.ReadingListPageContract
import org.wikipedia.dataclient.WikiSite

class ReadingListPageTable : DatabaseTable<ReadingListPage>(ReadingListPageContract.TABLE, ReadingListPageContract.URI) {
    override fun fromCursor(cursor: Cursor): ReadingListPage {
        val langCode = ReadingListPageContract.Col.LANG.value(cursor)
        val site = ReadingListPageContract.Col.SITE.value(cursor)
        val nameSpace = ReadingListPageContract.Col.NAMESPACE.value(cursor)
        val displayTitle = ReadingListPageContract.Col.DISPLAY_TITLE.value(cursor)
        val apiTitle = ReadingListPageContract.Col.API_TITLE.value(cursor).orEmpty().ifEmpty { displayTitle }
        return ReadingListPage(langCode?.let { WikiSite(site, it) } ?: WikiSite(site),
            nameSpace, displayTitle, apiTitle).apply {
            listId = ReadingListPageContract.Col.LISTID.value(cursor)
            id = ReadingListPageContract.Col.ID.value(cursor)
            description = ReadingListPageContract.Col.DESCRIPTION.value(cursor)
            thumbUrl = ReadingListPageContract.Col.THUMBNAIL_URL.value(cursor)
            atime = ReadingListPageContract.Col.ATIME.value(cursor)
            mtime = ReadingListPageContract.Col.MTIME.value(cursor)
            revId = ReadingListPageContract.Col.REVID.value(cursor)
            offline = ReadingListPageContract.Col.OFFLINE.value(cursor) != 0L
            status = ReadingListPageContract.Col.STATUS.value(cursor)
            sizeBytes = ReadingListPageContract.Col.SIZEBYTES.value(cursor)
            remoteId = ReadingListPageContract.Col.REMOTEID.value(cursor)
            lang = ReadingListPageContract.Col.LANG.value(cursor)
        }
    }

    override fun getColumnsAdded(version: Int): Array<Column<*>> {
        return when (version) {
            dBVersionIntroducedAt -> arrayOf(
                    ReadingListPageContract.Col.ID,
                    ReadingListPageContract.Col.LISTID,
                    ReadingListPageContract.Col.SITE,
                    ReadingListPageContract.Col.LANG,
                    ReadingListPageContract.Col.NAMESPACE,
                    ReadingListPageContract.Col.DISPLAY_TITLE,
                    ReadingListPageContract.Col.MTIME,
                    ReadingListPageContract.Col.ATIME,
                    ReadingListPageContract.Col.THUMBNAIL_URL,
                    ReadingListPageContract.Col.DESCRIPTION,
                    ReadingListPageContract.Col.REVID,
                    ReadingListPageContract.Col.OFFLINE,
                    ReadingListPageContract.Col.STATUS,
                    ReadingListPageContract.Col.SIZEBYTES,
                    ReadingListPageContract.Col.REMOTEID,
            )
            DB_VER_API_TITLE_ADDED -> arrayOf(ReadingListPageContract.Col.API_TITLE)
            else -> super.getColumnsAdded(version)
        }
    }

    public override fun onUpgradeSchema(db: SQLiteDatabase, fromVersion: Int, toVersion: Int) {
        if (toVersion == dBVersionIntroducedAt) {
            val currentLists = mutableListOf<ReadingList>()
            createDefaultList(db, currentLists)
            renameListsWithIdenticalNameAsDefault(db, currentLists)
            // TODO: add other one-time conversions here.
        }
    }

    public override fun toContentValues(obj: ReadingListPage) = contentValuesOf(
            ReadingListPageContract.Col.LISTID.name to obj.listId,
            ReadingListPageContract.Col.SITE.name to obj.wiki.authority(),
            ReadingListPageContract.Col.LANG.name to obj.wiki.languageCode(),
            ReadingListPageContract.Col.NAMESPACE.name to obj.namespace.code(),
            ReadingListPageContract.Col.DISPLAY_TITLE.name to obj.displayTitle,
            ReadingListPageContract.Col.API_TITLE.name to obj.apiTitle,
            ReadingListPageContract.Col.MTIME.name to obj.mtime,
            ReadingListPageContract.Col.ATIME.name to obj.atime,
            ReadingListPageContract.Col.THUMBNAIL_URL.name to obj.thumbUrl,
            ReadingListPageContract.Col.DESCRIPTION.name to obj.description,
            ReadingListPageContract.Col.REVID.name to obj.revId,
            ReadingListPageContract.Col.OFFLINE.name to if (obj.offline) 1 else 0,
            ReadingListPageContract.Col.STATUS.name to obj.status,
            ReadingListPageContract.Col.SIZEBYTES.name to obj.sizeBytes,
            ReadingListPageContract.Col.REMOTEID.name to obj.remoteId
    )

    override fun getPrimaryKeySelection(obj: ReadingListPage, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, ReadingListPageContract.Col.SELECTION)
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: ReadingListPage): Array<String?> {
        return arrayOf(obj.displayTitle)
    }

    private fun createDefaultList(db: SQLiteDatabase, currentLists: MutableList<ReadingList>) {
        for (list in currentLists) {
            if (list.isDefault) {
                // Already have a default list
                return
            }
        }
        ReadingListDbHelper.run {
            currentLists.add(createDefaultList(db))
        }
    }

    private fun renameListsWithIdenticalNameAsDefault(db: SQLiteDatabase, lists: List<ReadingList>) {
        ReadingListDbHelper.run {
            for (list in lists) {
                if (list.dbTitle.equals(WikipediaApp.getInstance().getString(R.string.default_reading_list_name), true)) {
                    list.dbTitle = WikipediaApp.getInstance().getString(R.string.reading_list_saved_list_rename, list.dbTitle)
                    updateList(db, list, false)
                }
            }
        }
    }

    override val dBVersionIntroducedAt = 18

    companion object {
        private const val DB_VER_API_TITLE_ADDED = 19
    }
}
