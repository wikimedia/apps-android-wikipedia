package org.wikipedia.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

public class PageContainerLongPressHandler implements LongPressHandler.OverflowMenuListener,
        LongPressHandler.WebViewOverflowMenuListener{
    @NonNull
    private final PageFragment fragment;

    public PageContainerLongPressHandler(@NonNull PageFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onOpenLink(PageTitle title, HistoryEntry entry) {
        fragment.loadPage(title, entry);
    }

    @Override
    public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
        fragment.openInNewBackgroundTab(title, entry);
    }

    @Override
    public void onCopyLink(PageTitle title) {
        copyLink(title.getCanonicalUri());
        showCopySuccessMessage();
    }

    @Override
    public void onShareLink(PageTitle title) {
        ShareUtil.shareText(fragment.getActivity(), title);
    }

    @Override
    public void onAddToList(PageTitle title, InvokeSource source) {
        fragment.addToReadingList(title, source);
    }

    @Override
    public WikiSite getWikiSite() {
        return fragment.getTitleOriginal().getWikiSite();
    }

    @Nullable
    @Override
    public String getReferrer() {
        return fragment.getTitle() != null ? fragment.getTitle().getCanonicalUri() : null;
    }

    private void copyLink(String url) {
        ClipboardUtil.setPlainText(fragment.getActivity(), null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(fragment.getActivity(), R.string.address_copied);
    }
}
