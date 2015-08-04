package org.wikipedia.pageimages;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.support.annotation.Nullable;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.data.PersistenceHelper;

public class PageImagePersistenceHelper extends PersistenceHelper<PageImage> {

    private static final String COL_SITE = "site";
    private static final String COL_TITLE = "title";
    private static final String COL_IMAGE_NAME = "imageName";

    @Override
    public PageImage fromCursor(Cursor c) {
        Site site = new Site(c.getString(c.getColumnIndex(COL_SITE)));
        // FIXME: Does not handle non mainspace pages
        PageTitle title = new PageTitle(null, c.getString(c.getColumnIndex(COL_TITLE)), site);
        String imageName = c.getString(c.getColumnIndex(COL_IMAGE_NAME));
        return new PageImage(title, imageName);
    }

    @Override
    protected ContentValues toContentValues(PageImage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_SITE, obj.getTitle().getSite().getDomain());
        contentValues.put(COL_TITLE, obj.getTitle().getPrefixedText());
        contentValues.put(COL_IMAGE_NAME, obj.getImageName());
        return contentValues;
    }

    @Nullable
    public String getImageUrlForTitle(WikipediaApp app, PageTitle title) {
        Cursor c = null;
        String thumbnail = null;
        try {
            String searchStr = title.getPrefixedText().replace("'", "''");
            String selection = getTableName() + "." + COL_TITLE + "='" + searchStr + "'";
            c = app.getPersister(PageImage.class).select(
                    selection, new String[] {}, "");
            if (c.getCount() > 0) {
                c.moveToFirst();
                thumbnail = c.getString(c.getColumnIndex("imageName"));
            }
        } catch (SQLiteException e) {
            // page title doesn't exist in database... no problem if it fails.
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return thumbnail;
    }

    @Override
    public String getTableName() {
        return "pageimages";
    }

    @Override
    public Column[] getColumnsAdded(int version) {
        switch (version) {
            case 1:
                return new Column[] {
                        new Column("_id", "integer primary key"),
                        new Column(COL_SITE, "string"),
                        new Column(COL_TITLE, "string"),
                        new Column(COL_IMAGE_NAME, "string"),
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
    protected String[] getPrimaryKeySelectionArgs(PageImage obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getPrefixedText()
        };
    }
}
