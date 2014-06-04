package org.wikipedia.beta.editing;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.ObservableWebView;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.R;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.ViewAnimations;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.bridge.CommunicationBridge;
import org.wikipedia.beta.bridge.StyleLoader;
import org.wikipedia.beta.editing.summaries.EditSummaryHandler;
import org.wikipedia.beta.history.HistoryEntry;
import org.wikipedia.beta.page.LinkHandler;
import org.wikipedia.beta.page.PageActivity;

import java.util.Locale;

public class EditPreviewFragment extends Fragment {
    private ObservableWebView webview;
    private View previewContainer;

    private String previewHTML;

    private CommunicationBridge bridge;
    private EditSummaryHandler editSummaryHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_preview_edit, container, false);
        webview = (ObservableWebView) parent.findViewById(R.id.edit_preview_webview);
        previewContainer = parent.findViewById(R.id.edit_preview_container);

        bridge = new CommunicationBridge(webview, "file:///android_asset/preview.html");

        return parent;
    }

    public void setEditSummaryHandler(EditSummaryHandler editSummaryHandler) {
        this.editSummaryHandler = editSummaryHandler;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            previewHTML = savedInstanceState.getString("previewHTML");
            boolean isActive = savedInstanceState.getBoolean("isActive");
            previewContainer.setVisibility(isActive ? View.VISIBLE : View.GONE);
            if (isActive) {
                displayPreview(previewHTML);
            }
        }
    }

    private void displayPreview(final String html) {
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.edit_preview_activity_title);
        ViewAnimations.crossFade(getActivity().findViewById(R.id.edit_section_container), previewContainer);
        JSONObject payload = new JSONObject();
        try {
            payload.put("html", html);
        } catch (JSONException e) {
            // Not happening
            throw new RuntimeException(e);
        }
        bridge.sendMessage("displayPreviewHTML", payload);

        editSummaryHandler.show();
    }

    private boolean isWebViewSetup = false;

    public void showPreview(PageTitle title, String wikiText) {
        Utils.hideSoftKeyboard(getActivity());
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.edit_preview_fetching_dialog_message));
        dialog.setCancelable(false);

        if (!isWebViewSetup) {
            Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);
            StyleLoader styleLoader = ((WikipediaApp)getActivity().getApplicationContext()).getStyleLoader();
            bridge.injectStyleBundle(styleLoader.getAvailableBundle(StyleLoader.BUNDLE_PREVIEW, title.getSite()));
            isWebViewSetup = true;
        }

        new LinkHandler(getActivity(), bridge, title.getSite()) {
            @Override
            public void onInternalLinkClicked(PageTitle title) {
                Intent intent = new Intent(getActivity(), PageActivity.class);
                intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                intent.putExtra(PageActivity.EXTRA_PAGETITLE, title);
                intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK));
                startActivity(intent);
            }
        };

        new EditPreviewTask(getActivity(), wikiText, title) {
            @Override
            public void onBeforeExecute() {
                dialog.show();
            }

            @Override
            public void onFinish(String result) {
                displayPreview(result);
                previewHTML = result;
                dialog.dismiss();
            }
        }.execute();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webview.destroy();
    }

    public boolean handleBackPressed() {
        if (isActive()) {
            hide();
            return editSummaryHandler.handleBackPressed();
        }
        return false;
    }

    public void hide() {
        ViewAnimations.crossFade(previewContainer, getActivity().findViewById(R.id.edit_section_container));
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(R.string.edit_section_activity_title);
    }


    public boolean isActive() {
        return previewContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("previewHTML", previewHTML);
        outState.putBoolean("isActive", isActive());
    }
}