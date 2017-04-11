package org.wikipedia.readinglist.sync;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;

import java.util.ArrayList;
import java.util.List;

public class RemoteReadingLists {
    // TODO: don't use userjs options for storing reading lists.
    // Since userjs options are not an optimal way of storing reading lists on the server,
    // we need to artificially limit the number of pages that can be synced.
    private static final int MAX_SYNCED_LISTS = 10;
    private static final int MAX_SYNCED_PAGES_PER_LIST = 100;

    @SuppressWarnings("unused") private long rev;
    @SuppressWarnings("unused") @NonNull private List<RemoteReadingList> lists;

    RemoteReadingLists(long rev, List<ReadingList> localLists) {
        this.rev = rev;
        lists = new ArrayList<>();
        for (ReadingList localList : localLists) {
            lists.add(new RemoteReadingList(localList));
            if (lists.size() >= MAX_SYNCED_LISTS) {
                break;
            }
        }
    }

    public long rev() {
        return rev;
    }

    @NonNull public List<RemoteReadingList> lists() {
        return lists;
    }

    static class RemoteReadingList {
        @SuppressWarnings("unused") @NonNull private String title;
        @SuppressWarnings("unused") @Nullable private String desc;
        @SuppressWarnings("unused") @NonNull private List<RemoteReadingListPage> pages;

        RemoteReadingList(@NonNull ReadingList localList) {
            this.title = localList.getTitle();
            this.desc = localList.getDescription();
            pages = new ArrayList<>();
            for (ReadingListPage localPage : localList.getPages()) {
                pages.add(new RemoteReadingListPage(localPage.wikiSite().languageCode(),
                        localPage.namespace().code(), localPage.title()));
                if (pages.size() >= MAX_SYNCED_PAGES_PER_LIST) {
                    break;
                }
            }
        }

        @NonNull public String title() {
            return title;
        }

        @NonNull public String desc() {
            return StringUtils.defaultString(desc);
        }

        @NonNull public List<RemoteReadingListPage> pages() {
            return pages;
        }
    }

    public static class RemoteReadingListPage {
        @NonNull private String lang;
        private int namespace;
        @NonNull private String title;

        RemoteReadingListPage(@NonNull String lang, int namespace, @NonNull String title) {
            this.lang = lang;
            this.namespace = namespace;
            this.title = title;
        }

        @NonNull public String lang() {
            return lang;
        }

        public int namespace() {
            return namespace;
        }

        @NonNull public String title() {
            return title;
        }
    }
}
