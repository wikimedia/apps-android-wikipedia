package org.wikipedia.readinglist.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.database.DatabaseTable;
import org.wikipedia.database.column.Column;
import org.wikipedia.database.contract.ReadingListContract;
import org.wikipedia.database.contract.SavedPageContract;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.readinglist.sync.ReadingListSynchronizer;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadingListTable extends DatabaseTable<ReadingListRow> {
    private static final int DB_VER_INTRODUCED = 13;
    private static final int DB_VER_SAVED_PAGES_MIGRATED = 14;
    private static final int DB_VER_READING_LISTS_REORGANIZED = 17;

    public ReadingListTable() {
        super(ReadingListContract.TABLE, ReadingListContract.List.URI);
    }

    @Override public ReadingListRow fromCursor(@NonNull Cursor cursor) {
        return ReadingListRow
                .builder()
                .key(ReadingListContract.List.KEY.val(cursor))
                .title(ReadingListContract.List.TITLE.val(cursor))
                .mtime(ReadingListContract.List.MTIME.val(cursor))
                .atime(ReadingListContract.List.ATIME.val(cursor))
                .description(ReadingListContract.List.DESCRIPTION.val(cursor))
                .build();
    }

    @NonNull @Override public Column<?>[] getColumnsAdded(int version) {
        switch (version) {
            case DB_VER_INTRODUCED:
                List<Column<?>> cols = new ArrayList<>();
                cols.add(ReadingListContract.List.ID);
                cols.add(ReadingListContract.List.KEY);
                cols.add(ReadingListContract.List.TITLE);
                cols.add(ReadingListContract.List.MTIME);
                cols.add(ReadingListContract.List.ATIME);
                cols.add(ReadingListContract.List.DESCRIPTION);
                return cols.toArray(new Column<?>[cols.size()]);
            default:
                return super.getColumnsAdded(version);
        }
    }

    @Override
    public void upgradeSchema(@NonNull SQLiteDatabase db, int fromVersion, int toVersion) {
        super.upgradeSchema(db, fromVersion, toVersion);
        if (toVersion == DB_VER_SAVED_PAGES_MIGRATED) {
            migrateSavedPages(db);
        } else if (toVersion == DB_VER_READING_LISTS_REORGANIZED) {
            reorganizeReadingListCache();
        }
    }

    @Override protected ContentValues toContentValues(@NonNull ReadingListRow row) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ReadingListContract.List.KEY.getName(), row.key());
        contentValues.put(ReadingListContract.List.TITLE.getName(), row.getTitle());
        contentValues.put(ReadingListContract.List.MTIME.getName(), row.mtime());
        contentValues.put(ReadingListContract.List.ATIME.getName(), row.atime());
        contentValues.put(ReadingListContract.List.DESCRIPTION.getName(), row.getDescription());
        return contentValues;
    }

    @Override protected String getPrimaryKeySelection(@NonNull ReadingListRow row,
                                                      @NonNull String[] selectionArgs) {
        return super.getPrimaryKeySelection(row, ReadingListContract.List.SELECTION);
    }

    @Override protected String[] getUnfilteredPrimaryKeySelectionArgs(@NonNull ReadingListRow row) {
        return new String[] {row.key()};
    }

    @Override protected int getDBVersionIntroducedAt() {
        return DB_VER_INTRODUCED;
    }

    private void migrateSavedPages(@NonNull SQLiteDatabase db) {
        Cursor cursor = SavedPage.DATABASE_TABLE.queryAll(db);
        try {
            if (cursor.getCount() == 0) {
                return;
            }

            String readingListTitle = WikipediaApp.getInstance().getString(R.string.nav_item_saved_pages);
            long now = System.currentTimeMillis();

            final ReadingList list = ReadingList
                    .builder()
                    .key(ReadingListDaoProxy.listKey(readingListTitle))
                    .title(readingListTitle)
                    .mtime(now)
                    .atime(now)
                    .description(null)
                    .pages(new ArrayList<ReadingListPage>())
                    .build();

            while (cursor.moveToNext()) {
                String title = SavedPageContract.Col.TITLE.val(cursor);
                String authority = SavedPageContract.Col.SITE.val(cursor);
                String lang = SavedPageContract.Col.LANG.val(cursor);
                String namespace = SavedPageContract.Col.NAMESPACE.val(cursor);
                WikiSite wiki = new WikiSite(authority, lang);
                PageTitle pageTitle = new PageTitle(namespace, title, null, null, wiki);

                list.add(ReadingListDaoProxy.page(list, pageTitle));
            }
            WikipediaApp.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    ReadingList.DAO.addList(list);
                }
            });

        } finally {
            cursor.close();
        }
    }

    private void reorganizeReadingListCache() {
        CallbackTask.execute(new CallbackTask.Task<Void>() {
            @Override public Void execute() throws Throwable {
                // Completely remove any contents from the old "savedpages" directory, since they
                // are now useless.
                FileUtil.deleteRecursively(new File(WikipediaApp.getInstance().getFilesDir(), "savedpages"));
                // Mark all reading list pages as outdated, so that they will be re-downloaded,
                // and thus re-cached in our new style, along with the newly-added size information.
                Cursor c = ReadingListPageDao.instance().allPages();
                try {
                    if (c.getCount() == 0) {
                        return null;
                    }
                    while (c.moveToNext()) {
                        ReadingListPage page = ReadingListPage.fromCursor(c);
                        ReadingListPageDao.instance().markOutdated(page);
                    }
                } finally {
                    c.close();
                }
                ReadingListSynchronizer.instance().syncSavedPages();
                return null;
            }
        }, null);
    }
}
