package org.wikipedia.readinglist.page.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.database.contract.ReadingListPageContract.PageCol;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.readinglist.page.ReadingListPageRow;

import java.util.ArrayList;
import java.util.List;

public class ReadingListPageTable extends DatabaseTable<ReadingListPageRow> {
    private static final int DB_VER_INTRODUCED = 12;
    private static final int DB_VER_SIZE_ADDED = 17;

    public ReadingListPageTable() {
        super(ReadingListPageContract.TABLE_PAGE, ReadingListPageContract.Page.URI);
    }

    @Override public ReadingListPageRow fromCursor(@NonNull Cursor cursor) {
        String lang = PageCol.LANG.val(cursor);
        String site = PageCol.SITE.val(cursor);
        return ReadingListPageRow
                .builder()
                .key(PageCol.KEY.val(cursor))
                .listKeys(PageCol.LIST_KEYS.val(cursor))
                .site(lang == null ? new WikiSite(site) : new WikiSite(site, lang))
                .namespace(PageCol.NAMESPACE.val(cursor))
                .title(PageCol.TITLE.val(cursor))
                .diskPageRevision(PageCol.DISK_PAGE_REV.val(cursor))
                .mtime(PageCol.MTIME.val(cursor))
                .atime(PageCol.ATIME.val(cursor))
                .thumbnailUrl(PageCol.THUMBNAIL_URL.val(cursor))
                .description(PageCol.DESCRIPTION.val(cursor))
                .physicalSize(PageCol.PHYSICAL_SIZE.val(cursor))
                .logicalSize(PageCol.LOGICAL_SIZE.val(cursor))
                .build();
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(PageCol.ID);
                cols.add(PageCol.KEY);
                cols.add(PageCol.LIST_KEYS);
                cols.add(PageCol.SITE);
                cols.add(PageCol.LANG);
                cols.add(PageCol.NAMESPACE);
                cols.add(PageCol.TITLE);
                cols.add(PageCol.DISK_PAGE_REV);
                cols.add(PageCol.MTIME);
                cols.add(PageCol.ATIME);
                cols.add(PageCol.THUMBNAIL_URL);
                cols.add(PageCol.DESCRIPTION);
                return cols.toArray(new Column<?>[cols.size()]);
            case DB_VER_SIZE_ADDED:
                return new Column<?>[]{PageCol.PHYSICAL_SIZE, PageCol.LOGICAL_SIZE};
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override protected ContentValues toContentValues(ReadingListPageRow row) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PageCol.KEY.getName(), row.key());
        PageCol.LIST_KEYS.put(contentValues, row.listKeys());
        contentValues.put(PageCol.SITE.getName(), row.wikiSite().authority());
        contentValues.put(PageCol.LANG.getName(), row.wikiSite().languageCode());
        contentValues.put(PageCol.NAMESPACE.getName(), row.namespace().code());
        contentValues.put(PageCol.TITLE.getName(), row.title());
        contentValues.put(PageCol.DISK_PAGE_REV.getName(), row.diskPageRevision());
        contentValues.put(PageCol.MTIME.getName(), row.mtime());
        contentValues.put(PageCol.ATIME.getName(), row.atime());
        contentValues.put(PageCol.THUMBNAIL_URL.getName(), row.thumbnailUrl());
        contentValues.put(PageCol.DESCRIPTION.getName(), row.description());
        contentValues.put(PageCol.PHYSICAL_SIZE.getName(), row.physicalSize());
        contentValues.put(PageCol.LOGICAL_SIZE.getName(), row.logicalSize());
        return contentValues;
    }

    @Override
    protected String getPrimaryKeySelection(@NonNull ReadingListPageRow row,
                                            @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, ReadingListPageContract.Page.SELECTION);
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull ReadingListPageRow row) {
        return new String[] {row.key()};
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }
}
