package org.wikipedia.beta.bookmarks;

import android.content.ContentValues;
import android.database.Cursor;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.data.PersistanceHelper;

import java.util.Date;

public class BookmarkPersistanceHelper extends PersistanceHelper<Bookmark> {
    @Override
    public Bookmark fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        Site site = new Site(c.getString(1));
        // FIXME: Does not handle non mainspace pages
        PageTitle title = new PageTitle(null, c.getString(2), site);
        Date timestamp = new Date(c.getLong(3));
        return new Bookmark(title, timestamp);
    }

    @Override
    protected ContentValues toContentValues(Bookmark obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("title", obj.getTitle().getPrefixedText());
        contentValues.put("timestamp", obj.getTimestamp().getTime());
        contentValues.put("site", obj.getTitle().getSite().getDomain());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return "bookmarks";
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return 3;
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
    protected String[] getPrimaryKeySelectionArgs(Bookmark obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getPrefixedText()
        };
    }


}
