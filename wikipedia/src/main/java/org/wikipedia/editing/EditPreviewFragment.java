package org.wikipedia.editing;

import android.app.*;
import android.os.*;
import android.support.v4.app.Fragment;
import android.view.*;
import org.json.*;
import org.wikipedia.*;
import org.wikipedia.editing.summaries.*;

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
        getActivity().getActionBar().setTitle(R.string.edit_preview_activity_title);
        Utils.crossFade(getActivity().findViewById(R.id.edit_section_container), previewContainer);
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

        if (!isDirectionSetup) {
            Utils.setupDirectionality(title.getSite().getLanguage(), bridge);
            isDirectionSetup = true;
        }

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
        if (previewContainer.getVisibility() == View.VISIBLE) {
            Utils.crossFade(previewContainer, getActivity().findViewById(R.id.edit_section_container));
            getActivity().getActionBar().setTitle(R.string.edit_section_activity_title);
            return true && editSummaryHandler.handleBackPressed();
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("previewHTML", previewHTML);
    }
}