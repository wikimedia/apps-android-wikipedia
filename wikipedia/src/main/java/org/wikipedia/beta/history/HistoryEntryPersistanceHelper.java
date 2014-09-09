package org.wikipedia.beta.history;

import android.content.ContentValues;
import android.database.Cursor;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.data.PersistanceHelper;

import java.util.Date;

public class HistoryEntryPersistanceHelper extends PersistanceHelper<HistoryEntry> {

    private static final int COL_INDEX_SITE = 1;
    private static final int COL_INDEX_TITLE = 2;
    private static final int COL_INDEX_TIMESTAMP = 3;
    private static final int COL_INDEX_SOURCE = 4;

    @Override
    public HistoryEntry fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        Site site = new Site(c.getString(COL_INDEX_SITE));
        // FIXME: Does not handle non mainspace pages
        PageTitle title = new PageTitle(null, c.getString(COL_INDEX_TITLE), site);
        Date timestamp = new Date(c.getLong(COL_INDEX_TIMESTAMP));
        int source = c.getInt(COL_INDEX_SOURCE);
        return new HistoryEntry(title, timestamp, source);
    }

    @Override
    protected ContentValues toContentValues(HistoryEntry obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("site", obj.getTitle().getSite().getDomain());
        contentValues.put("title", obj.getTitle().getPrefixedText());
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
