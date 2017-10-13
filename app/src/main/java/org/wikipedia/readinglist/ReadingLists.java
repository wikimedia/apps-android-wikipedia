package org.wikipedia.readinglist;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReadingLists {
    public static final int SORT_BY_NAME_ASC = 0;
    public static final int SORT_BY_NAME_DESC = 1;
    public static final int SORT_BY_RECENT_ASC = 2;
    public static final int SORT_BY_RECENT_DESC = 3;

    @NonNull private List<ReadingList> lists = new ArrayList<>();

    public void set(@NonNull List<ReadingList> lists) {
        this.lists = lists;
    }

    @NonNull public List<String> getTitles() {
        List<String> result = new ArrayList<>();
        for (ReadingList list : lists) {
            result.add(list.getTitle());
        }
        return result;
    }

    @NonNull public List<String> getTitlesExcept(@NonNull String title) {
        List<String> result = getTitles();
        result.remove(title);
        return result;
    }

    public ReadingList get(int pos) {
        return lists.get(pos);
    }

    @Nullable public ReadingList get(@Nullable String title) {
        for (ReadingList list : lists) {
            if (list.getTitle().equals(title)) {
                return list;
            }
        }
        return null;
    }

    public List<ReadingList> get() {
        return lists;
    }

    public int size() {
        return lists.size();
    }

    public boolean isEmpty() {
        return lists.isEmpty();
    }

    public void sort(int sortMode) {
        switch (sortMode) {
            case SORT_BY_NAME_ASC:
                Collections.sort(lists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                });
                break;
            case SORT_BY_NAME_DESC:
                Collections.sort(lists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return rhs.getTitle().compareTo(lhs.getTitle());
                    }
                });
                break;
            case SORT_BY_RECENT_ASC:
                Collections.sort(lists, new Comparator<ReadingList>() {
                    @Override
                    public int compare(ReadingList lhs, ReadingList rhs) {
                        return ((Long) lhs.atime()).compareTo(rhs.atime());
                    }
                });
                break;
            case SORT_BY_RECENT_DESC:
                Collections.sort(lists, new Comparator<ReadingList>() {
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
