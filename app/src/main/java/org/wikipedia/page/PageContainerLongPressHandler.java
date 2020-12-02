package org.wikipedia.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

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
        fragment.addToReadingList(entry.getTitle(), Constants.InvokeSource.CONTEXT_MENU);
    }

    @Override
    public void onMoveRequest(@Nullable ReadingListPage page, @NonNull HistoryEntry entry) {
        fragment.moveToReadingList(page.listId(), entry.getTitle(), Constants.InvokeSource.CONTEXT_MENU, true);
    }

    @Override
    public void onDeleted(@Nullable ReadingListPage page, @NonNull HistoryEntry entry) {
        // TODO: add remove page method
    }

    @Override
    public void onCopyLink(@NonNull HistoryEntry entry) {
        copyLink(entry.getTitle().getUri());
        showCopySuccessMessage();
    }

    @Override
    public void onShareLink(@NonNull HistoryEntry entry) {
        ShareUtil.shareText(fragment.getActivity(), entry.getTitle());
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

    private void copyLink(String url) {
        ClipboardUtil.setPlainText(fragment.getActivity(), null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(fragment.getActivity(), R.string.address_copied);
    }
}
