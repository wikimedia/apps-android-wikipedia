package org.wikipedia.database.column

import android.database.Cursor

class StrColumn(tbl: String, name: String, type: String) : Column<String?>(tbl, name, type) {
    override fun value(cursor: Cursor): String {
        return getString(cursor)
    }
}