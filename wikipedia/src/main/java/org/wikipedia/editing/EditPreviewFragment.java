package org.wikipedia.editing;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.support.v7.app.*;
import android.view.*;
import org.json.*;
import org.wikipedia.*;
import org.wikipedia.editing.summaries.*;
import org.wikipedia.history.*;
import org.wikipedia.page.*;

import java.util.*;

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
            displayPreview(previewHTML);
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

    private boolean isDirectionSetup = false;

    public void showPreview(PageTitle title, String wikiText) {
        Utils.hideSoftKeyboard(getActivity());
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(R.string.edit_preview_fetching_dialog_message));
        dialog.setCancelable(false);

        if (!isDirectionSetup) {
            Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);
            isDirectionSetup = true;
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
    }
}