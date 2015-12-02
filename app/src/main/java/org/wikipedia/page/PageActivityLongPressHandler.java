package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.server.PageService;
import org.wikipedia.server.ContentServiceFactory;
import org.wikipedia.savedpages.SaveOtherPageCallback;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

public abstract class PageActivityLongPressHandler implements PageLongPressHandler.ContextMenuListener {
    @NonNull private final PageActivity activity;

    public PageActivityLongPressHandler(@NonNull PageActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onOpenLink(PageTitle title, HistoryEntry entry) {
        activity.loadPage(title, entry);
    }

    @Override
    public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
        activity.loadPage(title, entry, PageActivity.TabPosition.NEW_TAB_BACKGROUND, false);
    }

    @Override
    public void onCopyLink(PageTitle title) {
        copyLink(title.getCanonicalUri());
        showCopySuccessMessage();
    }

    @Override
    public void onShareLink(PageTitle title) {
        ShareUtil.shareText(activity, title);
    }

    @Override
    public void onSavePage(PageTitle title) {
        saveOtherPage(title);
    }

    private void copyLink(String url) {
        ClipboardUtil.setPlainText(activity, null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(activity, R.string.address_copied);
    }

    private void saveOtherPage(@NonNull final PageTitle title) {
        getApiService(title).pageCombo(title.getPrefixedText(),
                !WikipediaApp.getInstance().isImageDownloadEnabled(),
                new SaveOtherPageCallback(title) {
                    @Override
                    protected void onComplete() {
                        if (!activity.isDestroyed()) {
                            activity.showPageSavedMessage(title.getDisplayText(), true);
                        }
                    }

                    @Override
                    protected void onError() {
                        FeedbackUtil.showMessage(activity, R.string.error_network_error_try_again);
                    }
                });
    }

    private PageService getApiService(PageTitle title) {
        return ContentServiceFactory.create(title.getSite());
    }
}
