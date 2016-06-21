package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.MainActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

public abstract class MainActivityLongPressHandler implements PageLongPressHandler.ContextMenuListener {
    @NonNull
    private final MainActivity activity;

    public MainActivityLongPressHandler(@NonNull MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onOpenLink(PageTitle title, HistoryEntry entry) {
        activity.loadPage(title, entry);
    }

    @Override
    public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
        activity.loadPage(title, entry, MainActivity.TabPosition.NEW_TAB_BACKGROUND, false);
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
    public void onAddToList(PageTitle title, AddToReadingListDialog.InvokeSource source) {
        activity.showAddToListDialog(title, source);
    }

    private void copyLink(String url) {
        ClipboardUtil.setPlainText(activity, null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(activity, R.string.address_copied);
    }
}
