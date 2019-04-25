package org.wikipedia;

import android.net.Uri;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;

import static org.wikipedia.Constants.InvokeSource.CONTEXT_MENU;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.isValidPageLink;

public class LongPressHandler implements View.OnLongClickListener {
    private final DialogMenuListener dialogMenuListener;
    private final int historySource;
    @Nullable
    private final String referrer;

    private PageTitle title;
    private HistoryEntry entry;

    public LongPressHandler(@NonNull View view, int historySource, @Nullable String referrer,
                            @NonNull DialogMenuListener listener) {
        this.historySource = historySource;
        this.dialogMenuListener = listener;
        this.referrer = referrer;
        view.setOnLongClickListener(this);
    }

    public LongPressHandler(@NonNull View view, int historySource,
                            @NonNull DialogMenuListener listener) {
        this(view, historySource, null, listener);
    }

    @Override
    public boolean onLongClick(View view) {
        title = null;
        if (view instanceof WebView) {
            WebView.HitTestResult result = ((WebView) view).getHitTestResult();
            if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                Uri uri = Uri.parse(result.getExtra());
                if (isValidPageLink(uri)) {
                    WikiSite wikiSite = new WikiSite(uri);
                    // the following logic keeps the correct language code if the domain has multiple variants (e.g. zh).
                    if (wikiSite.dbName().equals(((WebViewDialogMenuListener) dialogMenuListener).getWikiSite().dbName())
                            && !wikiSite.languageCode().equals(((WebViewDialogMenuListener) dialogMenuListener).getWikiSite().languageCode())) {
                        wikiSite = ((WebViewDialogMenuListener) dialogMenuListener).getWikiSite();
                    }
                    title = wikiSite.titleForInternalLink(uri.getPath());
                }
            }
        } else if (view instanceof ListView) {
            title = ((ListViewDialogMenuListener) dialogMenuListener).getTitleForListPosition(((ListView) view).getSelectedItemPosition());
        }

        if (title != null && !title.isSpecial()) {
            hideSoftKeyboard(view);
            entry = new HistoryEntry(title, historySource);
            entry.setReferrer(referrer);

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(title.getDisplayText());
            builder.setItems(R.array.long_press_handler_array, (dialog, which) -> {
                switch (which) {
                    case 0:
                        dialogMenuListener.onOpenLink(title, entry);
                        break;
                    case 1:
                        dialogMenuListener.onOpenInNewTab(title, entry);
                        break;
                    case 2:
                        dialogMenuListener.onAddToList(title, CONTEXT_MENU);
                        break;
                    case 3:
                        dialogMenuListener.onShareLink(title);
                        break;
                    case 4:
                        dialogMenuListener.onCopyLink(title);
                        break;
                    default:
                        break;
                }
            });

            builder.show();
        }
        return false;
    }

    public interface DialogMenuListener {
        void onOpenLink(PageTitle title, HistoryEntry entry);
        void onOpenInNewTab(PageTitle title, HistoryEntry entry);
        void onCopyLink(PageTitle title);
        void onShareLink(PageTitle title);
        void onAddToList(PageTitle title, InvokeSource source);
    }

    public interface ListViewDialogMenuListener extends DialogMenuListener {
        PageTitle getTitleForListPosition(int position);
    }

    public interface WebViewDialogMenuListener extends DialogMenuListener {
        WikiSite getWikiSite();
    }
}
