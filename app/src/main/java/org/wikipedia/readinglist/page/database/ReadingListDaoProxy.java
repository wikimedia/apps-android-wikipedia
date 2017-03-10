package org.wikipedia.readinglist.page.database;

import android.support.annotation.NonNull;
import android.util.Base64;

import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.disk.DiskStatus;

public final class ReadingListDaoProxy {

    @NonNull public static PageTitle pageTitle(@NonNull ReadingListPage page) {
        return new PageTitle(page.title(), page.wikiSite(), page.thumbnailUrl(), page.description());
    }

    @NonNull public static ReadingListPage page(@NonNull ReadingList list, @NonNull PageTitle title) {
        long now = System.currentTimeMillis();
        return ReadingListPage
                .builder()
                .diskStatus(DiskStatus.OUTDATED)
                .key(key(title))
                .listKeys(listKey(list))
                .site(title.getWikiSite())
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
        // return "wikipedia-" + title.getWikiSite().languageCode() + '-' + title.getProperties().getPageId();
        return Base64.encodeToString((title.getWikiSite().languageCode() + '-' + title.getDisplayText()).getBytes(),
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

    @NonNull public static String listName(@NonNull String key) {
        return new String(Base64.decode(key, Base64.NO_WRAP));
    }

    private ReadingListDaoProxy() { }
}
