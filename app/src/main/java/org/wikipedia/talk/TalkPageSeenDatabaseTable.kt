package org.wikipedia.talk

import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.core.content.contentValuesOf
import org.wikipedia.WikipediaApp
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.DbUtil
import org.wikipedia.database.column.Column
import org.wikipedia.database.column.LongColumn
import org.wikipedia.database.column.StrColumn
import org.wikipedia.database.contract.AppContentProviderContract
import org.wikipedia.dataclient.page.TalkPage.Topic

private const val TABLE = "talkpageseen"
private const val PATH = "talkpage/seen"
private val URI = Uri.withAppendedPath(AppContentProviderContract.AUTHORITY_BASE, PATH)

object TalkPageSeenDatabaseTable : DatabaseTable<String>(TABLE, URI) {
    private const val DB_VER_INTRODUCED = 21

    override val dBVersionIntroducedAt: Int
        get() = DB_VER_INTRODUCED

    private val ID = LongColumn(TABLE, BaseColumns._ID, "integer primary key")
    private val SHA = StrColumn(TABLE, "sha", "string")
    private val SELECTION = DbUtil.qualifiedNames(SHA)

    override fun fromCursor(cursor: Cursor): String {
        return SHA.value(cursor)
    }

    override fun toContentValues(obj: String) = contentValuesOf(SHA.name to obj)

    override fun getColumnsAdded(version: Int): Array<Column<*>> {
        return when (version) {
            DB_VER_INTRODUCED -> arrayOf(ID, SHA)
            else -> super.getColumnsAdded(version)
        }
    }

    override fun getPrimaryKeySelection(obj: String, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, SELECTION)
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: String): Array<String?> {
        return arrayOf(obj)
    }

    fun isTalkTopicSeen(topic: Topic): Boolean {
        val db = WikipediaApp.getInstance().database.readableDatabase
        db.query(TABLE, null, SHA.name + " = ?", arrayOf(topic.getIndicatorSha()),
                null, null, null).use { cursor ->
            if (cursor.moveToNext()) {
                return true
            }
        }
        return false
    }

    fun setTalkTopicSeen(topic: Topic) {
        val db = WikipediaApp.getInstance().database.writableDatabase
        db.beginTransaction()
        try {
            db.insertOrThrow(TABLE, null, toContentValues(topic.getIndicatorSha()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun resetAllUnseen() {
        val db = WikipediaApp.getInstance().database.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM $TABLE")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
