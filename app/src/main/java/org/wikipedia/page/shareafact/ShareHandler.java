package org.wikipedia.page.shareafact;

import android.app.SearchManager;
import android.content.Intent;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ShareAFactFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.log.L;
import org.wikipedia.wiktionary.WiktionaryDialog;

import java.util.Arrays;
import java.util.Locale;

import io.reactivex.disposables.CompositeDisposable;

/**
 * Let user choose between sharing as text or as image.
 */
public class ShareHandler {
    private static final String PAYLOAD_PURPOSE_SHARE = "share";
    private static final String PAYLOAD_PURPOSE_DEFINE = "define";
    private static final String PAYLOAD_PURPOSE_EDIT_HERE = "edit_here";
    private static final String PAYLOAD_TEXT_KEY = "text";

    @NonNull private final PageFragment fragment;
    @NonNull private final CommunicationBridge bridge;
    @Nullable private ActionMode webViewActionMode;
    @Nullable private ShareAFactFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();

    private void createFunnel() {
        WikipediaApp app = WikipediaApp.getInstance();
        final Page page = fragment.getPage();
        final PageProperties pageProperties = page.getPageProperties();
        funnel = new ShareAFactFunnel(app, page.getTitle(), pageProperties.getPageId(),
                pageProperties.getRevisionId());
    }

    public ShareHandler(@NonNull PageFragment fragment, @NonNull CommunicationBridge bridge) {
        this.fragment = fragment;
        this.bridge = bridge;
    }

    public void dispose() {
        disposables.clear();
    }

    public void showWiktionaryDefinition(String text) {
        PageTitle title = fragment.getTitle();
        fragment.showBottomSheet(WiktionaryDialog.newInstance(title, text));
    }

    private void onEditHerePayload(int sectionID, String text, boolean isEditingDescription) {
        if (sectionID == 0 && isEditingDescription) {
            fragment.verifyBeforeEditingDescription(text);
        } else {
            if (sectionID >= 0) {
                fragment.getEditHandler().startEditingSection(sectionID, text);
            }
        }
    }

    /**
     * @param mode ActionMode under which this context is starting.
     */
    public void onTextSelected(ActionMode mode) {
        webViewActionMode = mode;
        onTextSelected(mode.getMenu(), false);
    }

    private void onTextSelected(@NonNull Menu menu, boolean isPopupMenu) {
        MenuItem defineItem = menu.findItem(R.id.menu_text_select_define);
        if (shouldEnableWiktionaryDialog()) {
            defineItem.setVisible(true);
            defineItem.setOnMenuItemClickListener(new RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_DEFINE));
        }
        MenuItem editItem = menu.findItem(R.id.menu_text_edit_here);
        editItem.setOnMenuItemClickListener(new RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_EDIT_HERE));
        if (!fragment.getPage().isArticle()) {
            editItem.setVisible(false);
        }

        if (isPopupMenu) {
            menu.findItem(R.id.menu_text_select_copy).setVisible(true);
            menu.findItem(R.id.menu_text_select_share).setVisible(true);
            menu.findItem(R.id.menu_text_select_web_search).setVisible(true);
        }

        if (funnel == null) {
            createFunnel();
        }
        funnel.logHighlight();
    }

    public void showPopupMenuOnTextSelected(@NonNull String selectedText, float x, float y) {
        PopupMenu popupMenu;
        View tempView = new View(fragment.getWebView().getContext());
        tempView.setX(x);
        tempView.setY(y);
        ((ViewGroup) fragment.getWebView().getRootView()).addView(tempView);
        popupMenu = new PopupMenu(fragment.getWebView().getContext(), tempView, 0);
        popupMenu.setOnDismissListener(menu -> ((ViewGroup) fragment.getWebView().getRootView()).removeView(tempView));
        popupMenu.getMenuInflater().inflate(R.menu.menu_text_select, popupMenu.getMenu());
        onTextSelected(popupMenu.getMenu(), true);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_text_select_copy:
                    fragment.getWebView().copyToClipboard();
                    return true;
                case R.id.menu_text_select_share:
                    sharePlainText(selectedText);
                    return true;
                case R.id.menu_text_select_web_search:
                    startWebSearch(selectedText);
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
    }

    public boolean shouldEnableWiktionaryDialog() {
        return Arrays.asList(WiktionaryDialog.getEnabledLanguages())
                .contains(fragment.getTitle().getWikiSite().languageCode());
    }

    private void sharePlainText(@NonNull String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        fragment.requireActivity().startActivity(intent);
    }

    private void startWebSearch(@NonNull String text) {
        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, text);
        fragment.requireActivity().startActivity(intent);
    }

    private void leaveActionMode() {
        if (hasWebViewActionMode()) {
            finishWebViewActionMode();
            nullifyWebViewActionMode();
        }
    }

    private boolean hasWebViewActionMode() {
        return webViewActionMode != null;
    }

    private void nullifyWebViewActionMode() {
        webViewActionMode = null;
    }

    private void finishWebViewActionMode() {
        webViewActionMode.finish();
    }

    private class RequestTextSelectOnMenuItemClickListener implements MenuItem.OnMenuItemClickListener {
        @NonNull private final String purpose;

        RequestTextSelectOnMenuItemClickListener(@NonNull String purpose) {
            this.purpose = purpose;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            // send an event to the WebView that will make it return the
            // selected text (or first paragraph) back to us...
            bridge.evaluate(JavaScriptActionHandler.getTextSelection(), value -> {
                if (!fragment.isAdded()) {
                    return;
                }
                leaveActionMode();
                JSONObject messagePayload;

                try {
                    messagePayload = new JSONObject(value);
                    String text = messagePayload.optString(PAYLOAD_TEXT_KEY, "");
                    switch (purpose) {
                        case PAYLOAD_PURPOSE_DEFINE:
                            showWiktionaryDefinition(text.toLowerCase(Locale.getDefault()));
                            break;
                        case PAYLOAD_PURPOSE_EDIT_HERE:
                            onEditHerePayload(messagePayload.optInt("section", 0), text, messagePayload.optBoolean("isTitleDescription", false));
                            break;
                        default:
                            L.d("Unknown purpose=" + purpose);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            });

            return true;
        }
    }
}
