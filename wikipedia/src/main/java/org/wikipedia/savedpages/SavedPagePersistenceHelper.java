package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.data.PersistenceHelper;

import java.util.Date;

public class SavedPagePersistenceHelper extends PersistenceHelper<SavedPage> {

    private static final int DB_VER_INTRODUCED = 4;

    private static final int COL_INDEX_SITE = 1;
    private static final int COL_INDEX_TITLE = 2;
    private static final int COL_INDEX_TIME = 3;

    @Override
    public SavedPage fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        Site site = new Site(c.getString(COL_INDEX_SITE));
        // FIXME: Does not handle non mainspace pages
        PageTitle title = new PageTitle(null, c.getString(COL_INDEX_TITLE), site);
        Date timestamp = new Date(c.getLong(COL_INDEX_TIME));
        return new SavedPage(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(SavedPage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", obj.getTitle().getPrefixedText());
        contentValues.put("timestamp", obj.getTimestamp().getTime());
        contentValues.put("site", obj.getTitle().getSite().getDomain());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "savedpages";
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    @Override
    public Column[] getColumnsAdded(int version) {
        switch (version) {
            case 1:
                return new Column[] {
                        new Column("_id", "integer primary key"),
                        new Column("site", "string"),
                        new Column("title", "string"),
                        new Column("timestamp", "integer")
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
    protected String[] getPrimaryKeySelectionArgs(SavedPage obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getPrefixedText()
        };
    }
}
