package org.wikipedia.editing;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.ApiException;
import org.wikipedia.NightModeHandler;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.EditFunnel;
import org.wikipedia.bridge.StyleBundle;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.editing.summaries.EditSummaryTag;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class EditPreviewFragment extends Fragment {
    private ObservableWebView webview;
    private ScrollView previewContainer;
    private EditSectionActivity parentActivity;

    private ViewGroup editSummaryTagsContainer;

    private String previewHTML;

    private CommunicationBridge bridge;

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
        PageTitle pageTitle = parentActivity.getPageTitle();
        funnel = WikipediaApp.getInstance().getFunnelManager().getEditFunnel(pageTitle);

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
        Locale newLocale = new Locale(pageTitle.getSite().languageCode());
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

        summaryTags = new ArrayList<>();
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
            bridge.injectStyleBundle(StyleBundle.getAvailableBundle(StyleBundle.BUNDLE_PREVIEW));
            L10nUtil.setupDirectionality(parentActivity.getPageTitle().getSite().languageCode(), Locale.getDefault().getLanguage(), bridge);
            if (WikipediaApp.getInstance().isCurrentThemeDark()) {
                new NightModeHandler(bridge).turnOn(false);
            }

            new LinkHandler(getActivity(), bridge) {
                @Override
                public void onPageLinkClicked(String href) {
                    // TODO: also need to handle references, issues, disambig, ... in preview eventually
                }

                @Override
                public void onUrlClick(final String href) {
                    // Check if this is an internal link, and if it is then open it internally
                    if (href.startsWith("/wiki/")) {
                        PageTitle title = getSite().titleForInternalLink(href);
                        onInternalLinkClicked(title);
                    } else {
                        //Show dialogue asking user to confirm they want to leave
                        showLeavingEditDialogue(new Runnable() {
                            @Override
                            public void run() {
                                openExternalLink(href);
                            }
                        });
                    }
                }

                @Override
                public void onInternalLinkClicked(final PageTitle title) {
                    //Show dialogue asking user to confirm they want to leave
                    showLeavingEditDialogue(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.setAction(MainActivity.ACTION_PAGE_FOR_TITLE);
                            intent.putExtra(MainActivity.EXTRA_PAGETITLE, title);
                            intent.putExtra(MainActivity.EXTRA_HISTORYENTRY, new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK));
                            startActivity(intent);
                        }
                    });
                }

                /**
                 * Shows the user a dialogue asking them if they really meant to leave the edit
                 * workflow, and warning them that their changes have not yet been saved.
                 * @param runnable The runnable that is run if the user chooses to leave.
                 */
                private void showLeavingEditDialogue(final Runnable runnable) {
                    //Ask the user if they really meant to leave the edit workflow
                    final AlertDialog leavingEditDialog = new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.dialog_message_leaving_edit)
                            .setPositiveButton(R.string.dialog_message_leaving_edit_leave, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //They meant to leave; close dialogue and run specified action
                                    dialog.dismiss();
                                    runnable.run();
                                }
                            })
                            .setNegativeButton(R.string.dialog_message_leaving_edit_stay, null)
                            .create();
                    leavingEditDialog.show();
                }

                /**
                 * Open an external link. The method uses the onUrlClick method in the superclass of
                 * of LinkHandler to do the heavy lifting. You can't call this method from inside a
                 * Runnable or an AlertDialog, so we put it in here instead.
                 * @param href The href of the external link to be opened.
                 */
                private void openExternalLink(String href) {
                    super.onUrlClick(href);
                }

                @Override
                public Site getSite() {
                    return parentActivity.getPageTitle().getSite();
                }
            };
            bridge.addListener("imageClicked", new CommunicationBridge.JSEventListener() {
                @Override
                public void onMessage(String messageType, JSONObject messagePayload) {
                    // just give it a stub message handler
                    // (images will not be clickable in Preview)
                }
            });
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
        hideSoftKeyboard(getActivity());

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

                if (!(caught instanceof ApiException)) {
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
