package org.wikipedia.database

import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wikipedia.database.column.Column
import org.wikipedia.util.log.L
import kotlin.math.max

abstract class DatabaseTable<T>(val tableName: String, val baseContentURI: Uri) {
    protected abstract val dBVersionIntroducedAt: Int

    private val dBVersionDroppedAt: Int
        get() = 0

    abstract fun fromCursor(cursor: Cursor): T

    protected abstract fun toContentValues(obj: T): ContentValues?

    open fun getColumnsAdded(version: Int): Array<Column<*>> {
        return emptyArray()
    }

    fun acquireClient(context: Context): ContentProviderClient? {
        return context.contentResolver.acquireContentProviderClient(baseContentURI)
    }

    /**
     * Get the db query string to be passed to the content provider where selecting for a null
     * value (including, notably, the main namespace) may be necessary.
     * @param obj The object on which the formatting of the string depends.
     * @return A SQL WHERE clause formatted for the content provider.
     */
    protected open fun getPrimaryKeySelection(obj: T, selectionArgs: Array<String>): String {
        var primaryKeySelection = ""
        val args = getUnfilteredPrimaryKeySelectionArgs(obj)
        for (i in args.indices) {
            primaryKeySelection += selectionArgs[i] + if (args[i] == null) " IS NULL" else " = ?"
            if (i < args.size - 1) {
                primaryKeySelection += " AND "
            }
        }
        return primaryKeySelection
    }

    /**
     * Get the selection arguments to be bound to the db query string.
     * @param obj The object from which selection args are derived.
     * @return The array of selection arguments with null values removed.  (Null arguments are
     * replaced with "IS NULL" in getPrimaryKeySelection(T obj, String[] selectionKeys).)
     */
    fun getPrimaryKeySelectionArgs(obj: T): Array<String> {
        return getUnfilteredPrimaryKeySelectionArgs(obj).filterNotNull().toTypedArray()
    }

    /**
     * Override to provide full list of selection arguments, including those which may have null
     * values, for use in constructing the SQL query string.
     * @param obj Object from which selection args are to be derived.
     * @return Array of selection arguments (including null values).
     */
    protected abstract fun getUnfilteredPrimaryKeySelectionArgs(obj: T): Array<String?>

    fun upgradeSchema(db: SupportSQLiteDatabase, fromVersion: Int, toVersion: Int) {
        if (fromVersion < dBVersionIntroducedAt) {
            createTables(db)
            onUpgradeSchema(db, fromVersion, dBVersionIntroducedAt)
        }
        for (ver in max(dBVersionIntroducedAt, fromVersion) + 1..toVersion) {
            L.i("ver=$ver")
            if (ver == dBVersionDroppedAt) {
                dropTable(db)
                break
            }
            for (column in getColumnsAdded(ver)) {
                val alterTableString = "ALTER TABLE $tableName ADD COLUMN $column"
                L.i(alterTableString)
                db.execSQL(alterTableString)
            }
            onUpgradeSchema(db, ver - 1, ver)
        }
    }

    protected open fun onUpgradeSchema(db: SupportSQLiteDatabase, fromVersion: Int, toVersion: Int) {}

    protected fun createTables(db: SupportSQLiteDatabase) {
        L.i("Creating table=$tableName")
        val cols = getColumnsAdded(dBVersionIntroducedAt)
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tableName + " ( " + cols.joinToString(", ") + " )")
    }

    private fun dropTable(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS $tableName")
        L.i("Dropped table=$tableName")
    }

    companion object {
        const val INITIAL_DB_VERSION = 1
    }
}
