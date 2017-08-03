package org.wikipedia.readinglist;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.wikipedia.database.contract.ReadingListContract;
import org.wikipedia.readinglist.database.ReadingListRow;
import org.wikipedia.readinglist.page.ReadingListPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.wikipedia.readinglist.ReadingLists.SORT_BY_NAME_ASC;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_NAME_DESC;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_RECENT_ASC;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_RECENT_DESC;

public final class ReadingList extends ReadingListRow {
    @NonNull private final List<ReadingListPage> pages;

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

    @Nullable public ReadingListPage get(int index) {
        if (index < 0 || index >= pages.size()) {
            return null;
        }
        return pages.get(index);
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
    }

    // The size of all resources in bytes.
    public long physicalSize() {
        long sum = 0;
        for (ReadingListPage page : pages) {
            sum += !page.isOffline() || page.physicalSize() == null ? 0 : page.physicalSize();
        }
        return sum;
    }

    // The size of all resources on disk in bytes.
    public long logicalSize() {
        long sum = 0;
        for (ReadingListPage page : pages) {
            sum += !page.isOffline() || page.logicalSize() == null ? 0 : page.logicalSize();
        }
        return sum;
    }

    public int pagesOffline() {
        int sum = 0;
        for (ReadingListPage page : pages) {
            sum += (page.isOffline() && !page.isSaving()) ? 1 : 0;
        }
        return sum;
    }

    public void setTitle(@NonNull String title) {
        title(title);
    }

    public void setDescription(@NonNull String description) {
        description(description);
    }

    public void sort(int sortMode) {
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
}
