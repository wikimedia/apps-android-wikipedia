package org.wikipedia.readinglist.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.ReadingListPageContract;
import org.wikipedia.dataclient.WikiSite;

import java.util.ArrayList;
import java.util.List;

public class ReadingListPageTable extends DatabaseTable<ReadingListPage> {
    private static final int DB_VER_INTRODUCED = 18;

    public ReadingListPageTable() {
        super(ReadingListPageContract.TABLE, ReadingListPageContract.URI);
    }

    @Override public ReadingListPage fromCursor(@NonNull Cursor cursor) {
        String lang = ReadingListPageContract.Col.LANG.val(cursor);
        String site = ReadingListPageContract.Col.SITE.val(cursor);
        ReadingListPage page = new ReadingListPage(lang == null ? new WikiSite(site) : new WikiSite(site, lang),
                ReadingListPageContract.Col.NAMESPACE.val(cursor),
                ReadingListPageContract.Col.TITLE.val(cursor),
                ReadingListPageContract.Col.LISTID.val(cursor));
        page.id(ReadingListPageContract.Col.ID.val(cursor));
        page.description(ReadingListPageContract.Col.DESCRIPTION.val(cursor));
        page.thumbUrl(ReadingListPageContract.Col.THUMBNAIL_URL.val(cursor));
        page.atime(ReadingListPageContract.Col.ATIME.val(cursor));
        page.mtime(ReadingListPageContract.Col.MTIME.val(cursor));
        page.revId(ReadingListPageContract.Col.REVID.val(cursor));
        page.offline(ReadingListPageContract.Col.OFFLINE.val(cursor) != 0);
        page.status(ReadingListPageContract.Col.STATUS.val(cursor));
        page.sizeBytes(ReadingListPageContract.Col.SIZEBYTES.val(cursor));
        page.remoteId(ReadingListPageContract.Col.REMOTEID.val(cursor));
        return page;
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(ReadingListPageContract.Col.ID);
                cols.add(ReadingListPageContract.Col.LISTID);
                cols.add(ReadingListPageContract.Col.SITE);
                cols.add(ReadingListPageContract.Col.LANG);
                cols.add(ReadingListPageContract.Col.NAMESPACE);
                cols.add(ReadingListPageContract.Col.TITLE);
                cols.add(ReadingListPageContract.Col.MTIME);
                cols.add(ReadingListPageContract.Col.ATIME);
                cols.add(ReadingListPageContract.Col.THUMBNAIL_URL);
                cols.add(ReadingListPageContract.Col.DESCRIPTION);
                cols.add(ReadingListPageContract.Col.REVID);
                cols.add(ReadingListPageContract.Col.OFFLINE);
                cols.add(ReadingListPageContract.Col.STATUS);
                cols.add(ReadingListPageContract.Col.SIZEBYTES);
                cols.add(ReadingListPageContract.Col.REMOTEID);
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    public void onUpgradeSchema(@NonNull SQLiteDatabase db, int fromVersion, int toVersion) {
        if (toVersion == DB_VER_INTRODUCED) {
            List<ReadingList> currentLists = new ArrayList<>();
            createDefaultList(db, currentLists);
            renameListsWithIdenticalNameAsDefault(db, currentLists);
            // TODO: add other one-time conversions here.
        }
    }

    @Override protected ContentValues toContentValues(@NonNull ReadingListPage row) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadingListPageContract.Col.LISTID.getName(), row.listId());
        contentValues.put(ReadingListPageContract.Col.SITE.getName(), row.wiki().authority());
        contentValues.put(ReadingListPageContract.Col.LANG.getName(), row.wiki().languageCode());
        contentValues.put(ReadingListPageContract.Col.NAMESPACE.getName(), row.namespace().code());
        contentValues.put(ReadingListPageContract.Col.TITLE.getName(), row.title());
        contentValues.put(ReadingListPageContract.Col.MTIME.getName(), row.mtime());
        contentValues.put(ReadingListPageContract.Col.ATIME.getName(), row.atime());
        contentValues.put(ReadingListPageContract.Col.THUMBNAIL_URL.getName(), row.thumbUrl());
        contentValues.put(ReadingListPageContract.Col.DESCRIPTION.getName(), row.description());
        contentValues.put(ReadingListPageContract.Col.REVID.getName(), row.revId());
        contentValues.put(ReadingListPageContract.Col.OFFLINE.getName(), row.offline() ? 1 : 0);
        contentValues.put(ReadingListPageContract.Col.STATUS.getName(), row.status());
        contentValues.put(ReadingListPageContract.Col.SIZEBYTES.getName(), row.sizeBytes());
        contentValues.put(ReadingListPageContract.Col.REMOTEID.getName(), row.remoteId());
        return contentValues;
    }

    @Override protected String getPrimaryKeySelection(@NonNull ReadingListPage row,
                                                      @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, ReadingListPageContract.Col.SELECTION);
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull ReadingListPage row) {
        return new String[] {row.title()};
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    private void createDefaultList(@NonNull SQLiteDatabase db, @NonNull List<ReadingList> currentLists) {
        for (ReadingList list : currentLists) {
            if (list.isDefault()) {
                // Already have a default list
                return;
            }
        }
        currentLists.add(ReadingListDbHelper.instance().createDefaultList(db));
    }

    private void renameListsWithIdenticalNameAsDefault(SQLiteDatabase db, List<ReadingList> lists) {
        for (ReadingList list : lists) {
            if (list.dbTitle().equalsIgnoreCase(WikipediaApp.getInstance().getString(R.string.default_reading_list_name))) {
                list.title(String.format(WikipediaApp.getInstance().getString(R.string.reading_list_saved_list_rename), list.dbTitle()));
                ReadingListDbHelper.instance().updateList(db, list, false);
            }
        }
    }
}
