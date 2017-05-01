package org.wikipedia.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wikipedia.WikipediaApp;
import org.wikipedia.edit.summaries.EditSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListRow;
import org.wikipedia.readinglist.page.ReadingListPageRow;
import org.wikipedia.savedpages.SavedPage;
import org.wikipedia.search.RecentSearch;
import org.wikipedia.useroption.database.UserOptionRow;
import org.wikipedia.util.log.L;

public class Database extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "wikipedia.db";
    private static final int DATABASE_VERSION = 17;

    private final DatabaseTable<?>[] databaseTables = {
            HistoryEntry.DATABASE_TABLE,
            PageImage.DATABASE_TABLE,
            RecentSearch.DATABASE_TABLE,
            SavedPage.DATABASE_TABLE,
            EditSummary.DATABASE_TABLE,

            // Order matters. UserOptionDatabaseTable has a dependency on
            // UserOptionHttpDatabaseTable table when upgrading so this table must appear before it.
            UserOptionRow.HTTP_DATABASE_TABLE,
            UserOptionRow.DATABASE_TABLE,

            ReadingListPageRow.DISK_DATABASE_TABLE,
            ReadingListPageRow.HTTP_DATABASE_TABLE,
            ReadingListPageRow.DATABASE_TABLE,

            ReadingListRow.DATABASE_TABLE
    };

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        for (DatabaseTable<?> table : databaseTables) {
            table.upgradeSchema(sqLiteDatabase, 0, DATABASE_VERSION);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int from, int to) {
        L.i("Upgrading from=" + from + " to=" + to);
        WikipediaApp.getInstance().putCrashReportProperty("fromDatabaseVersion", String.valueOf(from));
        for (DatabaseTable<?> table : databaseTables) {
            table.upgradeSchema(sqLiteDatabase, from, to);
        }
    }
}
