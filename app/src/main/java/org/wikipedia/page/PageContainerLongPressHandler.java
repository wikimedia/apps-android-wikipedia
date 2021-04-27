package org.wikipedia.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.LongPressHandler;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.readinglist.database.ReadingListPage;

public class PageContainerLongPressHandler implements LongPressHandler.WebViewMenuCallback {
    @NonNull
    private final PageFragment fragment;

    public PageContainerLongPressHandler(@NonNull PageFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onOpenLink(@NonNull HistoryEntry entry) {
        fragment.loadPage(entry.getTitle(), entry);
    }

    @Override
    public void onOpenInNewTab(@NonNull HistoryEntry entry) {
        fragment.openInNewBackgroundTab(entry.getTitle(), entry);
    }

    @Override
    public void onAddRequest(@NonNull HistoryEntry entry, boolean addToDefault) {
        fragment.addToReadingList(entry.getTitle(), Constants.InvokeSource.CONTEXT_MENU, addToDefault);
    }

    @Override
    public void onMoveRequest(@Nullable ReadingListPage page, @NonNull HistoryEntry entry) {
        fragment.moveToReadingList(page.getListId(), entry.getTitle(), Constants.InvokeSource.CONTEXT_MENU, true);
    }

    @NonNull
    @Override
    public WikiSite getWikiSite() {
        return fragment.getTitle().getWikiSite();
    }

    @Nullable
    @Override
    public String getReferrer() {
        return fragment.getTitle() != null ? fragment.getTitle().getUri() : null;
    }
}
