package org.wikipedia.page.shareafact;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import io.reactivex.rxjava3.disposables.CompositeDisposable;

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
        Menu menu = mode.getMenu();

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

        if (funnel == null) {
            createFunnel();
        }
        funnel.logHighlight();
    }

    public boolean shouldEnableWiktionaryDialog() {
        return Arrays.asList(WiktionaryDialog.getEnabledLanguages())
                .contains(fragment.getTitle().getWikiSite().languageCode());
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
