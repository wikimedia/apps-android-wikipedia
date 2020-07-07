package org.wikipedia.readinglist.database;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReadingList implements Serializable {
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

    public long getMTime() {
        return mtime;
    }
    public void setMTime(long mtime) {
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
                Collections.sort(list.pages(), Comparator.comparing(ReadingListPage::accentAndCaseInvariantTitle));
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(list.pages(), Comparator.comparing(ReadingListPage::accentAndCaseInvariantTitle).reversed());
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(list.pages(), Comparator.comparingLong(ReadingListPage::getMTime));
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(list.pages(), Comparator.comparingLong(ReadingListPage::getMTime).reversed());
                break;
            default:
                break;
        }
    }

    public static void sort(List<ReadingList> lists, int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(lists, Comparator.comparing(ReadingList::accentAndCaseInvariantTitle));
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(lists, Comparator.comparing(ReadingList::accentAndCaseInvariantTitle).reversed());
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(lists, Comparator.comparingLong(ReadingList::getMTime));
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(lists, Comparator.comparingLong(ReadingList::getMTime).reversed());
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

    public static void sortGenericList(List<Object> lists, int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(lists, (lhs, rhs) -> {
                    if (lhs instanceof ReadingList && rhs instanceof ReadingList) {
                        return ((ReadingList) lhs).accentAndCaseInvariantTitle().compareTo(((ReadingList) rhs).accentAndCaseInvariantTitle());
                    } else {
                        return 0;
                    }
                });
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(lists, (lhs, rhs) -> {
                    if (lhs instanceof ReadingList && rhs instanceof ReadingList) {
                        return ((ReadingList) rhs).accentAndCaseInvariantTitle().compareTo(((ReadingList) lhs).accentAndCaseInvariantTitle());
                    } else {
                        return 0;
                    }
                });
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(lists, (lhs, rhs) -> {
                    if (lhs instanceof ReadingList && rhs instanceof ReadingList) {
                        return Long.compare(((ReadingList) rhs).getMTime(), ((ReadingList) lhs).getMTime());
                    } else {
                        return 0;
                    }
                });
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(lists, (lhs, rhs) -> {
                    if (lhs instanceof ReadingList && rhs instanceof ReadingList) {
                        return Long.compare(((ReadingList) lhs).getMTime(), ((ReadingList) rhs).getMTime());
                    } else {
                        return 0;
                    }
                });
                break;
            default:
                break;
        }
        // make the Default list sticky on top, regardless of sorting.
        ReadingList defaultList = null;
        for (Object list : lists) {
            if (list instanceof ReadingList && ((ReadingList) list).isDefault()) {
                defaultList = (ReadingList) list;
                break;
            }
        }
        if (defaultList != null) {
            lists.remove(defaultList);
            lists.add(0, defaultList);
        }
    }
}
