package org.wikipedia.readinglist;

import android.database.Cursor;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.Validate;
import org.wikipedia.database.contract.ReadingListContract;
import org.wikipedia.readinglist.database.ReadingListRow;
import org.wikipedia.readinglist.page.ReadingListPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ReadingList extends ReadingListRow {
    public static final int SORT_BY_NAME_ASC = 0;
    public static final int SORT_BY_NAME_DESC = 1;
    public static final int SORT_BY_RECENT_ASC = 2;
    public static final int SORT_BY_RECENT_DESC = 3;

    @NonNull private final List<ReadingListPage> pages;
    private boolean emptyListSavePagesState = true;
    private int sortMode = SORT_BY_NAME_ASC;

    public static ReadingList fromCursor(@NonNull Cursor cursor) {
        ReadingListRow list = ReadingList.DATABASE_TABLE.fromCursor(cursor);
        List<ReadingListPage> pages = new ArrayList<>();

        cursor.moveToPrevious();
        while (cursor.moveToNext()) {
            ReadingListRow curList = ReadingList.DATABASE_TABLE.fromCursor(cursor);
            if (!curList.key().equals(list.key())) {
                cursor.moveToPrevious();
                break;
            }

            boolean hasRow = ReadingListContract.ListWithPagesAndDisk.PAGE_KEY.val(cursor) != null;
            if (hasRow) {
                ReadingListPage page = ReadingListPage.fromCursor(cursor);
                pages.add(page);
            }
        }

        return ReadingList
                .builder()
                .copy(list)
                .pages(pages)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull public List<ReadingListPage> getPages() {
        return pages;
    }

    public void remove(@NonNull ReadingListPage page) {
        for (ReadingListPage p : pages) {
            if (p.key().equals(page.key())) {
                pages.remove(p);
                return;
            }
        }
    }

    public void add(@NonNull ReadingListPage page) {
        for (ReadingListPage p : pages) {
            if (p.key().equals(page.key())) {
                return;
            }
        }
        pages.add(0, page);
        sortPages();
    }

    public void setTitle(@NonNull String title) {
        title(title);
    }

    public void setDescription(@NonNull String description) {
        description(description);
    }

    public void setSaveOffline(boolean saved) {
        emptyListSavePagesState = saved;

        for (ReadingListPage page : pages) {
            page.savedOrSaving(saved);
        }
    }

    public boolean getSaveOffline() {
        if (pages.isEmpty()) {
            return emptyListSavePagesState;
        }

        for (ReadingListPage page : pages) {
            if (!page.savedOrSaving()) {
                return false;
            }
        }
        return true;
    }

    public void setSort(int sortMode) {
        this.sortMode = sortMode;
        sortPages();
    }

    private void sortPages() {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(pages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return lhs.title().compareTo(rhs.title());
                    }
                });
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(pages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return rhs.title().compareTo(lhs.title());
                    }
                });
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(pages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return ((Long) lhs.atime()).compareTo(rhs.atime());
                    }
                });
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(pages, new Comparator<ReadingListPage>() {
                    @Override
                    public int compare(ReadingListPage lhs, ReadingListPage rhs) {
                        return ((Long) rhs.atime()).compareTo(lhs.atime());
                    }
                });
                break;
            default:
                break;
        }
    }

    private ReadingList(@NonNull Builder builder) {
        super(builder);
        pages = new ArrayList<>(builder.pages);
        sortPages();
    }

    public static class Builder extends ReadingListRow.Builder<Builder> {
        private List<ReadingListPage> pages;

        public Builder pages(@NonNull List<ReadingListPage> pages) {
            this.pages = new ArrayList<>(pages);
            return this;
        }

        @Override public ReadingList build() {
            validate();
            return new ReadingList(this);
        }

        @Override protected void validate() {
            super.validate();
            Validate.notNull(pages);
        }
    }

    public static void sortReadingLists(List<ReadingList> readingLists, int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(readingLists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                });
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(readingLists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return rhs.getTitle().compareTo(lhs.getTitle());
                    }
                });
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(readingLists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return ((Long) lhs.atime()).compareTo(rhs.atime());
                    }
                });
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(readingLists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return ((Long) rhs.atime()).compareTo(lhs.atime());
                    }
                });
                break;
            default:
                break;
        }
    }
}