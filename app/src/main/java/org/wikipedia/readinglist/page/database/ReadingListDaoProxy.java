package org.wikipedia.readinglist.page.database;

import android.support.annotation.NonNull;
import android.util.Base64;

import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ReadingListDaoProxy {

    public static List<PageTitle> pageTitles(@NonNull Collection<ReadingListPage> pages) {
        List<PageTitle> titles = new ArrayList<>();
        for (ReadingListPage page : pages) {
            titles.add(pageTitle(page));
        }
        return titles;
    }

    @NonNull public static PageTitle pageTitle(@NonNull ReadingListPage page) {
        return new PageTitle(page.title(), page.site(), page.thumbnailUrl(), page.description());
    }

    @NonNull public static ReadingListPage page(@NonNull ReadingList list, @NonNull PageTitle title) {
        long now = System.currentTimeMillis();
        return ReadingListPage
                .builder()
                .diskStatus(list.getSaveOffline() ? DiskStatus.OUTDATED : DiskStatus.ONLINE)
                .key(key(title))
                .listKeys(listKey(list))
                .site(title.getSite())
                .namespace(title.namespace())
                .title(title.getDisplayText())
                .diskPageRevision(title.hasProperties() ? title.getProperties().getRevisionId() : 0)
                .mtime(now)
                .atime(now)
                .thumbnailUrl(title.hasProperties() ? title.getProperties().getLeadImageUrl() : null)
                .description(title.getDescription())
                .build();
    }

    @NonNull public static String key(@NonNull PageTitle title) {
        // TODO: this should use the following but PageTitles often do not have Properties and page
        //       ID is not preserved elsewhere.
        // return "wikipedia-" + title.getSite().languageCode() + '-' + title.getProperties().getPageId();
        return Base64.encodeToString((title.getSite().languageCode() + '-' + title.getDisplayText()).getBytes(),
                Base64.NO_WRAP);
    }

    @NonNull public static String listKey(@NonNull ReadingList list) {
        // TODO: we need to rekey all pages if a user changes the list title.
        return listKey(list.getTitle());
    }

    @NonNull public static String listKey(@NonNull String title) {
        // TODO: we need to rekey all pages if a user changes the list title.
        return Base64.encodeToString(title.getBytes(), Base64.NO_WRAP);
    }

    private ReadingListDaoProxy() { }
}
