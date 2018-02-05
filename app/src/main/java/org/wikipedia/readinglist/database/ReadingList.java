package org.wikipedia.readinglist.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadingList {
    public static final int SORT_BY_NAME_ASC = 0;
    public static final int SORT_BY_NAME_DESC = 1;
    public static final int SORT_BY_RECENT_ASC = 2;
    public static final int SORT_BY_RECENT_DESC = 3;
    public static final ReadingListTable DATABASE_TABLE = new ReadingListTable();

    private List<ReadingListPage> pages = new ArrayList<>();

    private long id;
    @NonNull private String title;
    @Nullable private String description;
    private long mtime;
    private long atime;
    private long sizeBytes;
    private boolean dirty = true;
    private long remoteId;

    @Nullable private transient String accentAndCaseInvariantTitle;

    protected ReadingList(@NonNull String title, @Nullable String description) {
        this.title = title;
        this.description = description;
        long now = System.currentTimeMillis();
        mtime = now;
        atime = now;
    }

    public List<ReadingListPage> pages() {
        return pages;
    }

    public int numPagesOffline() {
        int count = 0;
        for (ReadingListPage page : pages) {
            if (page.offline() && page.status() == ReadingListPage.STATUS_SAVED) {
                count++;
            }
        }
        return count;
    }

    public boolean isDefault() {
        return TextUtils.isEmpty(title);
    }

    public long id() {
        return id;
    }
    public void id(long id) {
        this.id = id;
    }

    @NonNull public String title() {
        return isDefault() ? WikipediaApp.getInstance().getString(R.string.default_reading_list_name) : title;
    }
    public void title(@NonNull String title) {
        this.title = title;
    }
    public String dbTitle() {
        return title;
    }

    @NonNull public String accentAndCaseInvariantTitle() {
        if (accentAndCaseInvariantTitle == null) {
            accentAndCaseInvariantTitle = StringUtils.stripAccents(title).toLowerCase();
        }
        return accentAndCaseInvariantTitle;
    }

    @Nullable public String description() {
        return description;
    }
    public void description(@Nullable String description) {
        this.description = description;
    }

    public long mtime() {
        return mtime;
    }
    public void mtime(long mtime) {
        this.mtime = mtime;
    }

    public long atime() {
        return atime;
    }
    public void atime(long atime) {
        this.atime = atime;
    }

    public void touch() {
        atime = System.currentTimeMillis();
    }

    public long sizeBytes() {
        long bytes = 0;
        for (ReadingListPage page : pages) {
            bytes += page.offline() ? page.sizeBytes() : 0;
        }
        return bytes;
    }
    public void sizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean dirty() {
        return dirty;
    }
    public void dirty(boolean dirty) {
        this.dirty = dirty;
    }

    public long remoteId() {
        return remoteId;
    }
    public void remoteId(long remoteId) {
        this.remoteId = remoteId;
    }

    public static void sort(ReadingList list, int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(list.pages(), (lhs, rhs) -> lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle()));
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(list.pages(), (lhs, rhs) -> rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle()));
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(list.pages(), (lhs, rhs) -> ((Long) lhs.mtime()).compareTo(rhs.mtime()));
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(list.pages(), (lhs, rhs) -> ((Long) rhs.mtime()).compareTo(lhs.mtime()));
                break;
            default:
                break;
        }
    }

    public static void sort(List<ReadingList> lists, int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(lists, (lhs, rhs) -> lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle()));
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(lists, (lhs, rhs) -> rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle()));
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(lists, (lhs, rhs) -> ((Long) lhs.mtime()).compareTo(rhs.mtime()));
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(lists, (lhs, rhs) -> ((Long) rhs.mtime()).compareTo(lhs.mtime()));
                break;
            default:
                break;
        }
        // make the Default list sticky on top, regardless of sorting.
        ReadingList defaultList = null;
        for (ReadingList list : lists) {
            if (list.isDefault()) {
                defaultList = list;
                break;
            }
        }
        if (defaultList != null) {
            lists.remove(defaultList);
            lists.add(0, defaultList);
        }
    }
}
