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

    private static final int COL_INDEX_SITE = 1;
    private static final int COL_INDEX_TITLE = 2;
    private static final int COL_INDEX_IMAGE_NAME = 3;

    @Override
    public PageImage fromCursor(Cursor c) {
        // Carefully, get them back by using position only
        Site site = new Site(c.getString(COL_INDEX_SITE));
        // FIXME: Does not handle non mainspace pages
        PageTitle title = new PageTitle(null, c.getString(COL_INDEX_TITLE), site);
        String imageName = c.getString(COL_INDEX_IMAGE_NAME);
        return new PageImage(title, imageName);
    }

    @Override
    protected ContentValues toContentValues(PageImage obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("site", obj.getTitle().getSite().getDomain());
        contentValues.put("title", obj.getTitle().getPrefixedText());
        contentValues.put("imageName", obj.getImageName());
        return contentValues;
    }

    @Nullable
    public String getImageUrlForTitle(WikipediaApp app, PageTitle title) {
        Cursor c = null;
        String thumbnail = null;
        try {
            String searchStr = title.getPrefixedText().replace("'", "''");
            String selection = getTableName() + ".title='" + searchStr + "'";
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
                        new Column("site", "string"),
                        new Column("title", "string"),
                        new Column("imageName", "string"),
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
    protected String[] getPrimaryKeySelectionArgs(PageImage obj) {
        return new String[] {
                obj.getTitle().getSite().getDomain(),
                obj.getTitle().getPrefixedText()
        };
    }
}
