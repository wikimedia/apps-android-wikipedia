package org.wikipedia.edit.preview;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.os.ConfigurationCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.EditFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.CommunicationBridge.CommunicationBridgeListener;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient;
import org.wikipedia.edit.EditSectionActivity;
import org.wikipedia.edit.summaries.EditSummaryTag;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.PageViewModel;
import org.wikipedia.page.references.PageReferences;
import org.wikipedia.page.references.ReferenceDialog;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewAnimations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

import static org.wikipedia.dataclient.RestService.PAGE_HTML_PREVIEW_ENDPOINT;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.handleExternalLink;

public class EditPreviewFragment extends Fragment implements CommunicationBridgeListener, ReferenceDialog.Callback {
    private ObservableWebView webview;
    private ScrollView previewContainer;

    private PageViewModel model = new PageViewModel();
    private CommunicationBridge bridge;
    private PageReferences references;
    private EditLinkHandler linkHandler;

    private List<EditSummaryTag> summaryTags;
    private EditSummaryTag otherTag;

    private EditFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_preview_edit, container, false);
        webview = parent.findViewById(R.id.edit_preview_webview);
        previewContainer = parent.findViewById(R.id.edit_preview_container);
        ViewGroup editSummaryTagsContainer = parent.findViewById(R.id.edit_summary_tags_container);
        bridge = new CommunicationBridge(this);
        linkHandler = new EditLinkHandler(requireActivity());
        initWebView();

        PageTitle pageTitle = ((EditSectionActivity)requireActivity()).getPageTitle();
        model.setTitle(pageTitle);
        model.setCurEntry(new HistoryEntry(pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK));
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
        Locale oldLocale = ConfigurationCompat.getLocales(oldResources.getConfiguration()).get(0);
        Locale newLocale = new Locale(pageTitle.getWikiSite().languageCode());
        Configuration config = new Configuration(oldResources.getConfiguration());
        Resources tempResources = getResources();
        boolean hasSameLocale = oldLocale.getLanguage().equals(newLocale.getLanguage());
        if (!hasSameLocale && !newLocale.getLanguage().equals("test")) {
            L10nUtil.setDesiredLocale(config, newLocale);
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
            tag.setOnClickListener((view) -> {
                funnel.logEditSummaryTap((Integer) view.getTag());
                tag.setSelected(!tag.getSelected());
            });
            editSummaryTagsContainer.addView(tag);
            summaryTags.add(tag);
        }

        otherTag = new EditSummaryTag(getActivity());
        otherTag.setText(tempResources.getString(R.string.edit_summary_tag_other));
        editSummaryTagsContainer.addView(otherTag);
        otherTag.setOnClickListener((view) -> {
            funnel.logEditSummaryTap(R.string.edit_summary_tag_other);
            if (otherTag.getSelected()) {
                otherTag.setSelected(false);
            } else {
                ((EditSectionActivity)requireActivity()).showCustomSummary();
            }
        });

        /*
        Reset AssetManager to its original state, by creating a new Resources object
        with the original Locale (from above)
         */
        if (!hasSameLocale) {
            config.setLocale(oldLocale);
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
        }
        previewContainer.setVisibility(View.GONE);
        return parent;
    }

    public void setCustomSummary(String summary) {
        otherTag.setText(summary.length() > 0 ? summary : getString(R.string.edit_summary_tag_other));
        otherTag.setSelected(summary.length() > 0);
    }

    /**
     * Fetches preview html from the modified wikitext text, and shows (fades in) the Preview fragment,
     * which includes edit summary tags. When the fade-in completes, the state of the
     * actionbar button(s) is updated, and the preview is shown.
     * @param title The PageTitle associated with the text being modified.
     * @param wikiText The text of the section to be shown in the Preview.
     */
    public void showPreview(final PageTitle title, final String wikiText) {
        hideSoftKeyboard(requireActivity());
        ((EditSectionActivity)requireActivity()).showProgressBar(true);

        String url = ServiceFactory.getRestBasePath(model.getTitle().getWikiSite()) + PAGE_HTML_PREVIEW_ENDPOINT + UriUtil.encodeURL(title.getPrefixedText());
        String postData = "wikitext=" + UriUtil.encodeURL(wikiText);
        webview.postUrl(url, postData.getBytes());

        ViewAnimations.fadeIn(previewContainer, () -> requireActivity().invalidateOptionsMenu());
        ViewAnimations.fadeOut(requireActivity().findViewById(R.id.edit_section_container));
    }

    private void initWebView() {
        webview.setWebViewClient(new OkHttpWebViewClient() {
            @NonNull @Override public PageViewModel getModel() {
                return model;
            }

            @NonNull @Override public LinkHandler getLinkHandler() {
                return linkHandler;
            }

            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!isAdded()) {
                    return;
                }
                ((EditSectionActivity)requireActivity()).showProgressBar(false);
                requireActivity().invalidateOptionsMenu();
                bridge.execute(JavaScriptActionHandler.setTopMargin(0));
            }
        });
        bridge.addListener("setup", (String messageType, JsonObject messagePayload) -> {
            // ignore
        });
        bridge.addListener("final_setup", (String messageType, JsonObject messagePayload) -> {
            // ignore
        });
        bridge.addListener("link", linkHandler);
        bridge.addListener("image", (messageType, messagePayload) -> {
            // TODO: do something when an image is clicked in Preview.
        });
        bridge.addListener("media", (messageType, messagePayload) -> {
            // TODO: do something when a video is clicked in Preview.
        });
        bridge.addListener("reference", (messageType, messagePayload) -> {
            references = GsonUtil.getDefaultGson().fromJson(messagePayload, PageReferences.class);
            if (!references.getReferencesGroup().isEmpty()) {
                bottomSheetPresenter.show(getChildFragmentManager(), new ReferenceDialog());
            }
        });
    }

    /**
     * Gets the overall edit summary, as specified by the user by clicking various tags,
     * and/or entering a custom summary.
     * @return Summary of the edit. If the user clicked more than one summary tag,
     * they will be separated by commas.
     */
    public String getSummary() {
        StringBuilder summaryStr = new StringBuilder();
        for (EditSummaryTag tag : summaryTags) {
            if (!tag.getSelected()) {
                continue;
            }
            if (summaryStr.length() > 0) {
                summaryStr.append(", ");
            }
            summaryStr.append(tag);
        }
        if (otherTag.getSelected()) {
            if (summaryStr.length() > 0) {
                summaryStr.append(", ");
            }
            summaryStr.append(otherTag);
        }
        return summaryStr.toString();
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        if (webview != null) {
            webview.clearAllListeners();
            ((ViewGroup) webview.getParent()).removeView(webview);
            bridge.cleanup();
            webview = null;
        }
        super.onDestroyView();
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
        View editSectionContainer = requireActivity().findViewById(R.id.edit_section_container);
        ViewAnimations.crossFade(previewContainer, editSectionContainer, () -> requireActivity().invalidateOptionsMenu());
    }

    public boolean isActive() {
        return previewContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        for (int i = 0; i < summaryTags.size(); i++) {
            outState.putBoolean("summaryTag" + i, summaryTags.get(i).getSelected());
        }
        if (otherTag.getSelected()) {
            outState.putString("otherTag", otherTag.toString());
        }
    }

    @Override
    public WebView getWebView() {
        return webview;
    }

    @Override
    public PageViewModel getModel() {
        return model;
    }

    @Override
    public boolean isPreview() {
        return true;
    }

    @Override
    public int getToolbarMargin() {
        return 0;
    }

    @Override
    public LinkHandler getLinkHandler() {
        return linkHandler;
    }

    @Override
    public List<PageReferences.Reference> getReferencesGroup() {
        return references.getReferencesGroup();
    }

    @Override
    public int getSelectedReferenceIndex() {
        return references.getSelectedIndex();
    }

    private class EditLinkHandler extends LinkHandler {
        EditLinkHandler(@NonNull Context context) {
            super(context);
        }

        @Override
        public void onPageLinkClicked(@NonNull String href, @NonNull String linkText) {
            // TODO: also need to handle references, issues, disambig, ... in preview eventually
        }

        @Override
        public void onInternalLinkClicked(@NonNull final PageTitle title) {
            showLeavingEditDialogue(() -> startActivity(PageActivity.newIntentForCurrentTab(getContext(),
                    new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK), title)));
        }

        @Override
        public void onExternalLinkClicked(@NonNull final Uri uri) {
            showLeavingEditDialogue(() -> handleExternalLink(getContext(), uri));
        }

        @Override
        public void onMediaLinkClicked(@NonNull PageTitle title) {
            // ignore
        }

        /**
         * Shows the user a dialogue asking them if they really meant to leave the edit
         * workflow, and warning them that their changes have not yet been saved.
         *
         * @param runnable The runnable that is run if the user chooses to leave.
         */
        private void showLeavingEditDialogue(final Runnable runnable) {
            //Ask the user if they really meant to leave the edit workflow
            final AlertDialog leavingEditDialog = new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.dialog_message_leaving_edit)
                    .setPositiveButton(R.string.dialog_message_leaving_edit_leave, (dialog, which) -> {
                        //They meant to leave; close dialogue and run specified action
                        dialog.dismiss();
                        runnable.run();
                    })
                    .setNegativeButton(R.string.dialog_message_leaving_edit_stay, null)
                    .create();
            leavingEditDialog.show();
        }

        @Override
        @NonNull
        public WikiSite getWikiSite() {
            return model.getTitle().getWikiSite();
        }

        @Override
        public void setWikiSite(@NotNull WikiSite wikiSite) {
            // ignore
        }
    }
}
