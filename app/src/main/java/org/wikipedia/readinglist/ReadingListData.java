package org.wikipedia.readinglist;

import android.support.annotation.NonNull;

import org.wikipedia.page.PageTitle;

@SuppressWarnings("checkstyle:interfaceistype")
public interface ReadingListData {
    interface List {
        @NonNull java.util.List<ReadingList> queryMruLists();
        void addList(ReadingList list);
        void removeList(ReadingList list);
        void makeListMostRecent(ReadingList list);
        void saveListInfo(ReadingList list);
    }

    interface Page {
        void addTitleToList(ReadingList list, PageTitle title);
        void removeTitleFromList(ReadingList list, PageTitle title);
        boolean listContainsTitle(ReadingList list, PageTitle title);
        boolean anyListContainsTitle(PageTitle title);
    }

    interface ReadingListDao extends List, Page {}
}