package org.wikipedia.database;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import org.jetbrains.annotations.NotNull;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.offline.OfflineObjectTable;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.log.L;

public class Database extends SupportSQLiteOpenHelper.Callback {
    private final SupportSQLiteOpenHelper.Callback callbackDelegate;

    private final DatabaseTable<?>[] databaseTables = {
            HistoryEntry.DATABASE_TABLE,
            PageImage.DATABASE_TABLE,
            ReadingList.DATABASE_TABLE,
            ReadingListPage.DATABASE_TABLE,
            OfflineObjectTable.DATABASE_TABLE
    };

    public Database(SupportSQLiteOpenHelper.Callback callbackDelegate) {
        super(callbackDelegate.version);
        this.callbackDelegate = callbackDelegate;
    }

    @Override
    public void onCreate(@NonNull @NotNull SupportSQLiteDatabase db) {
        callbackDelegate.onCreate(db);
        for (DatabaseTable<?> table : databaseTables) {
            table.upgradeSchema(db, 0, callbackDelegate.version);
        }
    }

    @Override
    public void onUpgrade(@NonNull @NotNull SupportSQLiteDatabase db, int from, int to) {
        L.i("Upgrading from=" + from + " to=" + to);
        callbackDelegate.onUpgrade(db, from, to);
        WikipediaApp.getInstance().putCrashReportProperty("fromDatabaseVersion", String.valueOf(from));
        for (DatabaseTable<?> table : databaseTables) {
            table.upgradeSchema(db, from, to);
        }
    }

    @Override
    public void onConfigure(@NonNull @NotNull SupportSQLiteDatabase db) {
        callbackDelegate.onConfigure(db);
    }

    @Override
    public void onCorruption(@NonNull @NotNull SupportSQLiteDatabase db) {
        callbackDelegate.onCorruption(db);
    }

    @Override
    public void onDowngrade(@NonNull @NotNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        callbackDelegate.onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(@NonNull @NotNull SupportSQLiteDatabase db) {
        callbackDelegate.onOpen(db);
    }
}
