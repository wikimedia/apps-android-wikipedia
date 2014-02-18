package org.wikipedia.history;

import android.content.*;
import android.database.*;
import org.wikipedia.*;
import org.wikipedia.data.*;

import java.util.*;

public class HistoryEntryPersistanceHelper extends PersistanceHelper<HistoryEntry> {
    @Override
    public HistoryEntry fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        Site site = new Site(c.getString(1));
        // FIXME: Does not handle non mainspace pages
        PageTitle title = new PageTitle(null, c.getString(2), site);
        Date timestamp = new Date(c.getLong(3));
        int source = c.getInt(4);
        return new HistoryEntry(title, timestamp, source);
    }

    @Override
    protected ContentValues toContentValues(HistoryEntry obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", obj.getTitle().getPrefixedText());
        contentValues.put("timestamp", obj.getTimestamp().getTime());
        contentValues.put("source", obj.getSource());
        contentValues.put("site", obj.getTitle().getSite().getDomain());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "history";
    }

    @Override
    public Column[] getColumnsAdded(int version) {
        switch (version) {
            case 1:
                return new Column[] {
                        new Column("_id", "integer primary key"),
                        new Column("site", "string"),
                        new Column("title", "string"),
                        new Column("timestamp", "integer"),
                        new Column("source", "integer"),
                };
            default:
                return new Column[0];
        }
    }

    @Override
    protected String getPrimaryKeySelection() {
        throw new UnsupportedOperationException("No Primary Keys make sense for History");
    }

    @Override
    protected String[] getPrimaryKeySelectionArgs(HistoryEntry obj) {
        throw new UnsupportedOperationException("No Primary Keys make sense for History");
    }


}
