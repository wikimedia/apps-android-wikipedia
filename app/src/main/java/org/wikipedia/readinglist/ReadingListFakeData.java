package org.wikipedia.readinglist;

import android.support.annotation.NonNull;

import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

public final class ReadingListFakeData implements ReadingListData.ReadingListDao {

    private static final List<ReadingList> LISTS = new ArrayList<>();

    @Override
    @NonNull
    public List<ReadingList> queryMruLists() {
        return LISTS;
    }

    @Override
    public void addList(ReadingList list) {
        LISTS.add(0, list);
    }

    @Override
    public void removeList(ReadingList list) {
        LISTS.remove(list);
    }

    @Override
    public void makeListMostRecent(ReadingList list) {
        LISTS.remove(list);
        LISTS.add(0, list);
    }

    @Override
    public void saveListInfo(ReadingList list) {
        // commit list details to DB. (name, description, whether saved offline)
    }

    @Override
    public void addTitleToList(ReadingList list, PageTitle title) {
        list.getPages().add(0, title);
    }

    @Override
    public void removeTitleFromList(ReadingList list, PageTitle title) {
        list.getPages().remove(title);
    }

    @Override
    public boolean listContainsTitle(ReadingList list, PageTitle title) {
        for (PageTitle p : list.getPages()) {
            if (p.equals(title)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean anyListContainsTitle(PageTitle title) {
        for (ReadingList list : LISTS) {
            if (listContainsTitle(list, title)) {
                return true;
            }
        }
        return false;
    }
}