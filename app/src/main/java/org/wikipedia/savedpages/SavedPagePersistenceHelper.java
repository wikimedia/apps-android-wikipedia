package org.wikipedia.savedpages;

import android.content.ContentValues;
import android.database.Cursor;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.data.PersistenceHelper;

import java.util.Date;

public class SavedPagePersistenceHelper extends PersistenceHelper<SavedPage> {

    private static final int DB_VER_INTRODUCED = 4;
    private static final int DB_VER_NAMESPACE_ADDED = 6;

    private static final String COL_SITE = "site";
    private static final String COL_TITLE = "title";
    private static final String COL_NAMESPACE = "namespace";
    private static final String COL_TIMESTAMP = "timestamp";

    @Override
    public SavedPage fromCursor(Cursor c) {
        Site site = new Site(c.getString(c.getColumnIndex(COL_SITE)));
        PageTitle title = new PageTitle(c.getString(c.getColumnIndex(COL_NAMESPACE)),
                c.getString(c.getColumnIndex(COL_TITLE)), site);
        Date timestamp = new Date(c.getLong(c.getColumnIndex(COL_TIMESTAMP)));
        return new SavedPage(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(SavedPage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_SITE, obj.getTitle().getSite().getDomain());
        contentValues.put(COL_TITLE, obj.getTitle().getText());
        contentValues.put(COL_NAMESPACE, obj.getTitle().getNamespace());
        contentValues.put(COL_TIMESTAMP, obj.getTimestamp().getTime());
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
                        new Column(COL_SITE, "string"),
                        new Column(COL_TITLE, "string"),
                        new Column(COL_TIMESTAMP, "integer")
                };
            case DB_VER_NAMESPACE_ADDED:
                return new Column[] {
                        new Column(COL_NAMESPACE, "string")
                };
            default:
                return new Column[0];
        }
    }

    @Override
    protected String getPrimaryKeySelection() {
        return COL_SITE + " = ? AND " + COL_TITLE + " = ?";
    }

    @Override
    protected String[] getPrimaryKeySelectionArgs(SavedPage obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getPrefixedText()
        };
    }
}
