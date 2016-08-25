package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.tabs.TabsProvider;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

public abstract class PageContainerLongPressHandler implements LongPressHandler.ContextMenuListener {
    @NonNull
    private final PageFragment.Callback container;

    public PageContainerLongPressHandler(@NonNull PageFragment.Callback container) {
        this.container = container;
    }

    @Override
    public void onOpenLink(PageTitle title, HistoryEntry entry) {
        container.onPageLoadPage(title, entry);
    }

    @Override
    public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
        container.onPageLoadPage(title, entry, TabsProvider.TabPosition.NEW_TAB_BACKGROUND);
    }

    @Override
    public void onCopyLink(PageTitle title) {
        copyLink(title.getCanonicalUri());
        showCopySuccessMessage();
    }

    @Override
    public void onShareLink(PageTitle title) {
        ShareUtil.shareText(container.getActivity(), title);
    }

    @Override
    public void onAddToList(PageTitle title, AddToReadingListDialog.InvokeSource source) {
        container.onPageAddToReadingList(title, source);
    }

    private void copyLink(String url) {
        ClipboardUtil.setPlainText(container.getActivity(), null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(container.getActivity(), R.string.address_copied);
    }
}
