package org.wikipedia;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;

import static org.wikipedia.Constants.InvokeSource.CONTEXT_MENU;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.isValidPageLink;

public class LongPressHandler implements View.OnCreateContextMenuListener,
        View.OnTouchListener, PopupMenu.OnMenuItemClickListener {
    private final OverflowMenuListener overflowMenuListener;
    private final int historySource;
    @Nullable private String referrer = null;
    private PageTitle title;
    private HistoryEntry entry;
    private float clickPositionX;
    private float clickPositionY;

    public LongPressHandler(@NonNull View view, int historySource, @NonNull OverflowMenuListener listener) {
        this.historySource = historySource;
        this.overflowMenuListener = listener;
        view.setOnCreateContextMenuListener(this);
        view.setOnTouchListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        title = null;
        if (view instanceof WebView) {
            WebView.HitTestResult result = ((WebView) view).getHitTestResult();
            if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                Uri uri = Uri.parse(result.getExtra());
                if (isValidPageLink(uri)) {
                    WikiSite wikiSite = new WikiSite(uri);
                    // the following logic keeps the correct language code if the domain has multiple variants (e.g. zh).
                    if (wikiSite.dbName().equals(((WebViewOverflowMenuListener) overflowMenuListener).getWikiSite().dbName())
                            && !wikiSite.languageCode().equals(((WebViewOverflowMenuListener) overflowMenuListener).getWikiSite().languageCode())) {
                        wikiSite = ((WebViewOverflowMenuListener) overflowMenuListener).getWikiSite();
                    }
                    title = wikiSite.titleForInternalLink(uri.getPath());
                    referrer = ((WebViewOverflowMenuListener) overflowMenuListener).getReferrer();
                    showPopupMenu(view, null);
                }
            }
        } else if (view instanceof ListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            title = ((ListViewOverflowMenuListener) overflowMenuListener).getTitleForListPosition(info.position);
            showPopupMenu(view, info);
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

    private void showPopupMenu(@NonNull View view, @Nullable AdapterView.AdapterContextMenuInfo info) {
        if (title != null && !title.isSpecial() && view.isAttachedToWindow()) {
            hideSoftKeyboard(view);
            entry = new HistoryEntry(title, historySource);
            entry.setReferrer(referrer);
            PopupMenu popupMenu;
            if (info == null) {
                View tempView = new View(view.getContext());
                tempView.setX(clickPositionX);
                tempView.setY(clickPositionY);
                ((ViewGroup) view.getRootView()).addView(tempView);
                popupMenu = new PopupMenu(view.getContext(), tempView, 0);
                popupMenu.setOnDismissListener(menu1 -> ((ViewGroup) view.getRootView()).removeView(tempView));
            } else {
                popupMenu = new PopupMenu(view.getContext(), info.targetView, Gravity.END);
            }
            popupMenu.getMenuInflater().inflate(R.menu.menu_page_long_press, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_long_press_open_page:
                overflowMenuListener.onOpenLink(title, entry);
                return true;
            case R.id.menu_long_press_open_in_new_tab:
                overflowMenuListener.onOpenInNewTab(title, entry);
                return true;
            case R.id.menu_long_press_copy_page:
                overflowMenuListener.onCopyLink(title);
                return true;
            case R.id.menu_long_press_share_page:
                overflowMenuListener.onShareLink(title);
                return true;
            case R.id.menu_long_press_add_to_list:
                overflowMenuListener.onAddToList(title, CONTEXT_MENU);
                return true;
            default:
            return false;
        }
    }

    public interface OverflowMenuListener {
        void onOpenLink(PageTitle title, HistoryEntry entry);
        void onOpenInNewTab(PageTitle title, HistoryEntry entry);
        void onCopyLink(PageTitle title);
        void onShareLink(PageTitle title);
        void onAddToList(PageTitle title, InvokeSource source);
    }

    public interface ListViewOverflowMenuListener extends OverflowMenuListener {
        PageTitle getTitleForListPosition(int position);
    }

    public interface WebViewOverflowMenuListener extends OverflowMenuListener {
        @NonNull WikiSite getWikiSite();
        @Nullable String getReferrer();
    }
}
