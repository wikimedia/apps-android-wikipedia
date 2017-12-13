package org.wikipedia.history;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.database.contract.PageHistoryContract.Col;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import java.util.Date;

public class HistoryEntryDatabaseTable extends DatabaseTable<HistoryEntry> {
    private static final int DB_VER_NAMESPACE_ADDED = 6;
    private static final int DB_VER_LANG_ADDED = 10;
    private static final int DB_VER_TIME_SPENT_ADDED = 15;

    public HistoryEntryDatabaseTable() {
        super(PageHistoryContract.TABLE, PageHistoryContract.Page.URI);
    }

    @Override
    public HistoryEntry fromCursor(Cursor cursor) {
        WikiSite wiki = new WikiSite(Col.SITE.val(cursor), Col.LANG.val(cursor));
        PageTitle title = new PageTitle(Col.NAMESPACE.val(cursor), Col.TITLE.val(cursor), wiki);
        Date timestamp = Col.TIMESTAMP.val(cursor);
        int source = Col.SOURCE.val(cursor);
        return new HistoryEntry(title, timestamp, source);
    }

    @Override
    protected ContentValues toContentValues(HistoryEntry obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Col.SITE.getName(), obj.getTitle().getWikiSite().authority());
        contentValues.put(Col.LANG.getName(), obj.getTitle().getWikiSite().languageCode());
        contentValues.put(Col.TITLE.getName(), obj.getTitle().getText());
        contentValues.put(Col.NAMESPACE.getName(), obj.getTitle().getNamespace());
        contentValues.put(Col.TIMESTAMP.getName(), obj.getTimestamp().getTime());
        contentValues.put(Col.SOURCE.getName(), obj.getSource());
        contentValues.put(Col.TIME_SPENT.getName(), obj.getTimeSpentSec());
        return contentValues;
    }

    @NonNull
    @Override
    public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case INITIAL_DB_VERSION:
                return new Column<?>[] {Col.ID, Col.SITE, Col.TITLE, Col.TIMESTAMP, Col.SOURCE};
            case DB_VER_NAMESPACE_ADDED:
                return new Column<?>[] {Col.NAMESPACE};
            case DB_VER_LANG_ADDED:
                return new Column<?>[] {Col.LANG};
            case DB_VER_TIME_SPENT_ADDED:
                return new Column<?>[] {Col.TIME_SPENT};
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull HistoryEntry obj,
                                            @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(obj, PageHistoryContract.Col.SELECTION);
    }

    @Override
    protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull HistoryEntry obj) {
        return new String[] {
                obj.getTitle().getWikiSite().authority(),
                obj.getTitle().getWikiSite().languageCode(),
                obj.getTitle().getNamespace(),
                obj.getTitle().getText()
        };
    }

    @Override
    protected int getDBVersionIntroducedAt() {
        return INITIAL_DB_VERSION;
    }
}
