package org.wikipedia.editing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import com.github.kevinsawicki.http.HttpRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.ApiException;
import org.wikipedia.*;
import org.wikipedia.analytics.EditFunnel;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.StyleLoader;
import org.wikipedia.editing.summaries.EditSummaryTag;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.PageActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditPreviewFragment extends Fragment {
    private ObservableWebView webview;
    private ScrollView previewContainer;
    private EditSectionActivity parentActivity;

    private ViewGroup editSummaryTagsContainer;

    private String previewHTML;

    private CommunicationBridge bridge;

    private NightModeHandler nightModeHandler;

    private List<EditSummaryTag> summaryTags;
    private EditSummaryTag otherTag;

    private ProgressDialog progressDialog;
    private EditFunnel funnel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_preview_edit, container, false);
        webview = (ObservableWebView) parent.findViewById(R.id.edit_preview_webview);
        previewContainer = (ScrollView) parent.findViewById(R.id.edit_preview_container);
        editSummaryTagsContainer = (ViewGroup) parent.findViewById(R.id.edit_summary_tags_container);

        bridge = new CommunicationBridge(webview, "file:///android_asset/preview.html");

        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        parentActivity = (EditSectionActivity)getActivity();
        funnel = WikipediaApp.getInstance().getFunnelManager().getEditFunnel(parentActivity.getPageTitle());

        /*
        Use a Resources object with a different Locale, so that the text of the canned summary
        buttons is shown in the selected Wiki language, instead of the current UI language.
        However, there's a caveat: creating a new Resources object actually modifies something
        internally in the AssetManager, so we'll need to create another new Resources object
        with the original Locale when we're done.
        https://code.google.com/p/android/issues/detail?id=67672
         */
        Resources oldResources = getResources();
        AssetManager assets = oldResources.getAssets();
        DisplayMetrics metrics = oldResources.getDisplayMetrics();
        Locale oldLocale = oldResources.getConfiguration().locale;
        Locale newLocale = new Locale(WikipediaApp.getInstance().getPrimaryLanguage());
        Configuration config = new Configuration(oldResources.getConfiguration());
        Resources tempResources = getResources();
        if (!oldLocale.getLanguage().equals(newLocale.getLanguage())) {
            config.locale = newLocale;
            tempResources = new Resources(assets, metrics, config);
        }

        // build up summary tags...
        int[] summaryTagStrings = {
                R.string.edit_summary_tag_typo,
                R.string.edit_summary_tag_grammar,
                R.string.edit_summary_tag_links
        };

        summaryTags = new ArrayList<EditSummaryTag>();
        for (int i : summaryTagStrings) {
            final EditSummaryTag tag = new EditSummaryTag(getActivity());
            tag.setText(tempResources.getString(i));
            tag.setTag(i);
            tag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    funnel.logEditSummaryTap((Integer) view.getTag());
                    tag.setSelected(!tag.getSelected());
                }
            });
            editSummaryTagsContainer.addView(tag);
            summaryTags.add(tag);
        }

        otherTag = new EditSummaryTag(getActivity());
        otherTag.setText(tempResources.getString(R.string.edit_summary_tag_other));
        editSummaryTagsContainer.addView(otherTag);
        otherTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                funnel.logEditSummaryTap(R.string.edit_summary_tag_other);
                if (otherTag.getSelected()) {
                    otherTag.setSelected(false);
                } else {
                    parentActivity.showCustomSummary();
                }
            }
        });

        /*
        Reset AssetManager to its original state, by creating a new Resources object
        with the original Locale (from above)
         */
        if (!oldLocale.getLanguage().equals(newLocale.getLanguage())) {
            config.locale = oldLocale;
            new Resources(assets, metrics, config);
        }

        if (savedInstanceState != null) {

            for (int i = 0; i < summaryTags.size(); i++) {
                summaryTags.get(i).setSelected(savedInstanceState.getBoolean("summaryTag" + i, false));
            }
            if (savedInstanceState.containsKey("otherTag")) {
                otherTag.setSelected(true);
                otherTag.setText(savedInstanceState.getString("otherTag"));
            }

            previewHTML = savedInstanceState.getString("previewHTML");
            boolean isActive = savedInstanceState.getBoolean("isActive");
            previewContainer.setVisibility(isActive ? View.VISIBLE : View.GONE);
            if (isActive) {
                displayPreview(previewHTML);
            }
        }

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.edit_preview_fetching_dialog_message));
        progressDialog.setCancelable(false);
    }

    public void setCustomSummary(String summary) {
        otherTag.setText(summary.length() > 0 ? summary : getString(R.string.edit_summary_tag_other));
        otherTag.setSelected(summary.length() > 0);
    }

    private boolean isWebViewSetup = false;

    private void displayPreview(final String html) {
        if (!isWebViewSetup) {
            isWebViewSetup = true;
            Utils.setupDirectionality(parentActivity.getPageTitle().getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);
            StyleLoader styleLoader = ((WikipediaApp) getActivity().getApplicationContext()).getStyleLoader();
            bridge.injectStyleBundle(styleLoader.getAvailableBundle(StyleLoader.BUNDLE_PREVIEW));
            if (WikipediaApp.getInstance().getCurrentTheme() == WikipediaApp.THEME_DARK) {
                nightModeHandler = new NightModeHandler(bridge);
                nightModeHandler.turnOn(false);
            }

            new LinkHandler(getActivity(), bridge) {
                @Override
                public void onPageLinkClicked(String href) {
                    // TODO: also need to handle references, issues, disambig, ... in preview eventually
                }

                @Override
                public void onInternalLinkClicked(PageTitle title) {
                    Intent intent = new Intent(getActivity(), PageActivity.class);
                    intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
                    intent.putExtra(PageActivity.EXTRA_PAGETITLE, title);
                    intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK));
                    startActivity(intent);
                }

                @Override
                public Site getSite() {
                    return parentActivity.getPageTitle().getSite();
                }
            };
        }

        ViewAnimations.fadeIn(previewContainer, new Runnable() {
            @Override
            public void run() {
                parentActivity.supportInvalidateOptionsMenu();
            }
        });
        ViewAnimations.fadeOut(getActivity().findViewById(R.id.edit_section_container));

        JSONObject payload = new JSONObject();
        try {
            payload.put("html", html);
        } catch (JSONException e) {
            // Not happening
            throw new RuntimeException(e);
        }
        bridge.sendMessage("displayPreviewHTML", payload);
    }

    /**
     * Fetches a preview of the modified text, and shows (fades in) the Preview fragment,
     * which includes edit summary tags. When the fade-in completes, the state of the
     * actionbar button(s) is updated, and the preview is shown.
     * @param title The PageTitle associated with the text being modified.
     * @param wikiText The text of the section to be shown in the Preview.
     */
    public void showPreview(final PageTitle title, final String wikiText) {
        Utils.hideSoftKeyboard(getActivity());

        new EditPreviewTask(getActivity(), wikiText, title) {
            @Override
            public void onBeforeExecute() {
                progressDialog.show();
            }

            @Override
            public void onFinish(String result) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                displayPreview(result);
                previewHTML = result;
                parentActivity.supportInvalidateOptionsMenu();
                progressDialog.dismiss();
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!progressDialog.isShowing()) {
                    // no longer attached to activity!
                    return;
                }
                progressDialog.dismiss();

                if (!(caught instanceof EditingException
                        || caught instanceof ApiException
                        || caught instanceof HttpRequest.HttpRequestException)) {
                    throw new RuntimeException(caught);
                }
                Log.d("Wikipedia", "Caught " + caught.toString());
                final AlertDialog retryDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.error_network_error)
                        .setPositiveButton(R.string.dialog_message_edit_failed_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showPreview(title, wikiText);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.dialog_message_edit_failed_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
                retryDialog.show();
            }
        }.execute();
    }

    /**
     * Gets the overall edit summary, as specified by the user by clicking various tags,
     * and/or entering a custom summary.
     * @return Summary of the edit. If the user clicked more than one summary tag,
     * they will be separated by commas.
     */
    public String getSummary() {
        String summaryStr = "";
        for (EditSummaryTag tag : summaryTags) {
            if (!tag.getSelected()) {
                continue;
            }
            if (summaryStr.length() > 0) {
                summaryStr += ", ";
            }
            summaryStr += tag;
        }
        if (otherTag.getSelected()) {
            if (summaryStr.length() > 0) {
                summaryStr += ", ";
            }
            summaryStr += otherTag;
        }
        return summaryStr;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webview.destroy();
    }

    public boolean handleBackPressed() {
        if (isActive()) {
            hide();
            return true;
        }
        return false;
    }

    /**
     * Hides (fades out) the Preview fragment.
     * When fade-out completes, the state of the actionbar button(s) is updated.
     */
    public void hide() {
        View editSectionContainer = getActivity().findViewById(R.id.edit_section_container);
        ViewAnimations.crossFade(previewContainer, editSectionContainer, new Runnable() {
            @Override
            public void run() {
                parentActivity.supportInvalidateOptionsMenu();
            }
        });
    }

    public boolean isActive() {
        return previewContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("previewHTML", previewHTML);
        outState.putBoolean("isActive", isActive());
        for (int i = 0; i < summaryTags.size(); i++) {
            outState.putBoolean("summaryTag" + i, summaryTags.get(i).getSelected());
        }
        if (otherTag.getSelected()) {
            outState.putString("otherTag", otherTag.toString());
        }
    }

    @Override
    public void onDetach() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDetach();
    }
}
