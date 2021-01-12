package org.wikipedia;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.LongPressMenu;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.isValidPageLink;

public class LongPressHandler implements View.OnCreateContextMenuListener, View.OnTouchListener {
    private final LongPressMenu.Callback callback;
    private final int historySource;
    @Nullable private String referrer = null;
    private PageTitle title;
    private HistoryEntry entry;
    private float clickPositionX;
    private float clickPositionY;

    public LongPressHandler(@NonNull View view, int historySource, @NonNull LongPressMenu.Callback callback) {
        this.historySource = historySource;
        this.callback = callback;
        view.setOnCreateContextMenuListener(this);
        view.setOnTouchListener(this);
    }

    public LongPressHandler(@NonNull View view, @NonNull PageTitle pageTitle, int historySource, @NonNull LongPressMenu.Callback callback) {
        this(view, historySource, callback);
        this.title = pageTitle;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        if (view instanceof WebView) {
            title = null;
            WebView.HitTestResult result = ((WebView) view).getHitTestResult();
            if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                Uri uri = Uri.parse(result.getExtra());
                if (isValidPageLink(uri)) {
                    WikiSite wikiSite = new WikiSite(uri);
                    // the following logic keeps the correct language code if the domain has multiple variants (e.g. zh).
                    if (wikiSite.dbName().equals(((WebViewMenuCallback) callback).getWikiSite().dbName())
                            && !wikiSite.languageCode().equals(((WebViewMenuCallback) callback).getWikiSite().languageCode())) {
                        wikiSite = ((WebViewMenuCallback) callback).getWikiSite();
                    }
                    title = wikiSite.titleForInternalLink(uri.getPath());
                    referrer = ((WebViewMenuCallback) callback).getReferrer();
                    showPopupMenu(view, true);
                }
            }
        } else {
            showPopupMenu(view, false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            clickPositionX = motionEvent.getX();
            clickPositionY = motionEvent.getY();
        }
        return false;
    }

    private void showPopupMenu(@NonNull View view, boolean createAnchorView) {
        if (title != null && !title.isSpecial() && view.isAttachedToWindow()) {
            hideSoftKeyboard(view);
            entry = new HistoryEntry(title, historySource);
            entry.setReferrer(referrer);
            View anchorView = view;
            if (createAnchorView) {
                View tempView = new View(view.getContext());
                tempView.setX(clickPositionX);
                tempView.setY(clickPositionY);
                ((ViewGroup) view.getRootView()).addView(tempView);
                anchorView = tempView;
            }

            new LongPressMenu(anchorView, true, callback).show(entry);
        }
    }

    public interface WebViewMenuCallback extends LongPressMenu.Callback {
        @NonNull WikiSite getWikiSite();
        @Nullable String getReferrer();
    }
}
