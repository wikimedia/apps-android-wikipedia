package org.wikipedia.database;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.database.contract.AppContentProviderContract;
import org.wikipedia.database.contract.EditHistoryContract;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum AppContentProviderEndpoint implements EnumCode {
    HISTORY_PAGE(100, PageHistoryContract.Page.PATH, PageHistoryContract.Page.TABLES,
            PageHistoryContract.Page.PROJECTION),
    HISTORY_PAGE_IMAGE(101, PageImageHistoryContract.Image.PATH,
            PageImageHistoryContract.Image.TABLES, PageImageHistoryContract.Image.PROJECTION),
    HISTORY_PAGE_WITH_IMAGE(102, PageHistoryContract.PageWithImage.PATH,
            PageHistoryContract.PageWithImage.TABLES, PageHistoryContract.PageWithImage.PROJECTION),
    HISTORY_EDIT_SUMMARY(103, EditHistoryContract.Summary.PATH, EditHistoryContract.Summary.TABLES,
            EditHistoryContract.Summary.PROJECTION),
    HISTORY_SEARCH_QUERY(104, SearchHistoryContract.Query.PATH, SearchHistoryContract.Query.TABLES,
            SearchHistoryContract.Query.PROJECTION);

    private static final EnumCodeMap<AppContentProviderEndpoint> CODE_TO_ENUM = new EnumCodeMap<>(AppContentProviderEndpoint.class);
    private static final UriMatcher URI_TO_CODE = newUriToCode();

    private final int code;
    @NonNull private final String authority;
    @NonNull private final String path;
    @NonNull private final String tables;
    @Nullable private final String[] projection;

    @NonNull public static AppContentProviderEndpoint of(@NonNull Uri uri) {
        int code = URI_TO_CODE.match(uri);
        if (code == UriMatcher.NO_MATCH) {
            throw new IllegalArgumentException("uri=" + uri);
        }

        return of(code);
    }

    @Override public int code() {
        return code;
    }

    @NonNull public String tables() {
        return tables;
    }

    @Nullable public String[] projection() {
        return projection;
    }

    @Nullable public String type() {
        return null;
    }

    @Nullable public Uri itemUri(@NonNull ContentValues values) {
        return null;
    }

    AppContentProviderEndpoint(int code, @NonNull String path, @NonNull String tables,
                               @Nullable String[] projection) {
        this(code, AppContentProviderContract.AUTHORITY, path, tables, projection);
    }

    AppContentProviderEndpoint(int code, @NonNull String authority, @NonNull String path,
                               @NonNull String tables, @Nullable String[] projection) {
        this.code = code;
        this.authority = authority;
        this.path = path;
        this.tables = tables;
        this.projection = projection;
    }

    @NonNull private static AppContentProviderEndpoint of(int code) {
        return CODE_TO_ENUM.get(code);
    }

    private static UriMatcher newUriToCode() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        for (AppContentProviderEndpoint value : AppContentProviderEndpoint.values()) {
            matcher.addURI(value.authority, value.path, value.code);
        }
        return matcher;
    }
}
