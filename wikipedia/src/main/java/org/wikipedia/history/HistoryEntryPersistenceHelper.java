package org.wikipedia.history;

import android.content.ContentValues;
import android.database.Cursor;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.data.PersistenceHelper;

import java.util.Date;

public class HistoryEntryPersistenceHelper extends PersistenceHelper<HistoryEntry> {

    private static final int DB_VER_NAMESPACE_ADDED = 6;

    private static final int COL_INDEX_SITE = 1;
    private static final int COL_INDEX_TITLE = 2;
    private static final int COL_INDEX_NAMESPACE = 3;
    private static final int COL_INDEX_TIMESTAMP = 4;
    private static final int COL_INDEX_SOURCE = 5;

    @Override
    public HistoryEntry fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        Site site = new Site(c.getString(COL_INDEX_SITE));
        PageTitle title = new PageTitle(c.getString(COL_INDEX_NAMESPACE), c.getString(COL_INDEX_TITLE), site);
        Date timestamp = new Date(c.getLong(COL_INDEX_TIMESTAMP));
        int source = c.getInt(COL_INDEX_SOURCE);
        return new HistoryEntry(title, timestamp, source);
    }

    @Override
    protected ContentValues toContentValues(HistoryEntry obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("site", obj.getTitle().getSite().getDomain());
        contentValues.put("title", obj.getTitle().getText());
        contentValues.put("namespace", obj.getTitle().getNamespace());
        contentValues.put("timestamp", obj.getTimestamp().getTime());
        contentValues.put("source", obj.getSource());
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
                        new Column("source", "integer")
                };
            case DB_VER_NAMESPACE_ADDED:
                return new Column[] {
                        new Column("namespace", "string")
                };
            default:
                return new Column[0];
        }
    }

    @Override
    protected String getPrimaryKeySelection() {
        return "site = ? AND title = ?";
    }

    @Override
    protected String[] getPrimaryKeySelectionArgs(HistoryEntry obj) {
        return new String[] {
            obj.getTitle().getSite().getDomain(),
            obj.getTitle().getPrefixedText()
        };
    }
}
