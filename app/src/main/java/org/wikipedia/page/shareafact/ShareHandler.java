package org.wikipedia.page.shareafact;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ShareAFactFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.gallery.ImageLicense;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.NoDimBottomSheetDialog;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.wiktionary.WiktionaryDialog;

import java.util.Arrays;
import java.util.Locale;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.analytics.ShareAFactFunnel.ShareMode;

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

    private void shareSnippet(@NonNull CharSequence input) {
        final String selectedText = StringUtil.sanitizeText(input.toString());
        final PageTitle title = fragment.getTitle();
        final String leadImageNameText = fragment.getPage().getPageProperties().getLeadImageName() != null
                ? fragment.getPage().getPageProperties().getLeadImageName() : "";
        final PageTitle imageTitle = new PageTitle(Namespace.FILE.toLegacyString(), leadImageNameText, title.getWikiSite());

        disposables.add(ServiceFactory.get(title.getWikiSite()).getImageExtMetadata(imageTitle.getPrefixedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> {
                    // noinspection ConstantConditions
                    MwQueryPage page = response.query().pages().get(0);
                    return page.imageInfo() != null && page.imageInfo().getMetadata() != null
                            ? new ImageLicense(page.imageInfo().getMetadata())
                            : new ImageLicense();
                })
                .subscribe(imageLicense -> {
                    final Bitmap snippetBitmap = SnippetImage.getSnippetImage(fragment.requireContext(),
                            fragment.getLeadImageBitmap(),
                            title.getDisplayText(),
                            fragment.getPage().isMainPage() ? "" : title.getDescription(),
                            selectedText,
                            imageLicense);
                    fragment.showBottomSheet(new PreviewDialog(fragment.getContext(),
                            snippetBitmap, title, selectedText, funnel));
                }, caught -> {
                    // If we failed to get license info for the lead image, just share the text
                    PreviewDialog.shareAsText(fragment.requireContext(), title, selectedText, funnel);
                    L.e("Error fetching image license info for " + title.getDisplayText(), caught);
                }));
    }

    /**
     * @param mode ActionMode under which this context is starting.
     */
    public void onTextSelected(ActionMode mode) {
        webViewActionMode = mode;
        Menu menu = mode.getMenu();
        MenuItem shareItem = menu.findItem(R.id.menu_text_select_share);

        // Provide our own listeners for the copy, define, and share buttons.
        shareItem.setOnMenuItemClickListener(new RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_SHARE));
        MenuItem copyItem = menu.findItem(R.id.menu_text_select_copy);
        copyItem.setOnMenuItemClickListener((MenuItem menuItem) -> {
            fragment.getWebView().copyToClipboard();
            FeedbackUtil.showMessage(fragment.getActivity(), R.string.text_copied);
            leaveActionMode();
            return true;
        });
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

    private boolean shouldEnableWiktionaryDialog() {
        return isWiktionaryDialogEnabledForArticleLanguage();
    }

    private boolean isWiktionaryDialogEnabledForArticleLanguage() {
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
                leaveActionMode();
                JSONObject messagePayload;

                try {
                    messagePayload = new JSONObject(value);
                    String text = messagePayload.optString(PAYLOAD_TEXT_KEY, "");
                    switch (purpose) {
                        case PAYLOAD_PURPOSE_SHARE:
                            if (funnel == null) {
                                createFunnel();
                            }
                            shareSnippet(text);
                            funnel.logShareTap(text);
                            break;
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

    /**
     * A dialog to be displayed before sharing with two action buttons:
     * "Share as image", "Share as text".
     */
    private static class PreviewDialog extends NoDimBottomSheetDialog {
        private boolean completed = false;

        PreviewDialog(final Context context, final Bitmap resultBitmap, final PageTitle title,
                      final String selectedText, final ShareAFactFunnel funnel) {
            super(context);
            View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_share_preview, null);
            setContentView(rootView);
            ImageView previewImage = rootView.findViewById(R.id.preview_img);
            previewImage.setImageBitmap(resultBitmap);
            rootView.findViewById(R.id.close_button)
                    .setOnClickListener((v) -> dismiss());
            rootView.findViewById(R.id.share_as_image_button)
                    .setOnClickListener((v) -> {
                        String introText = context.getString(R.string.snippet_share_intro,
                                title.getDisplayText(),
                                UriUtil.getUrlWithProvenance(context, title, R.string.prov_share_image));
                        ShareUtil.shareImage(context, resultBitmap, title.getDisplayText(),
                                title.getDisplayText(), introText);
                        funnel.logShareIntent(selectedText, ShareMode.image);
                        completed = true;
                    });
            rootView.findViewById(R.id.share_as_text_button)
                    .setOnClickListener((v) -> {
                        shareAsText(context, title, selectedText, funnel);
                        completed = true;
                    });
            setOnDismissListener((dialog) -> {
                resultBitmap.recycle();
                if (!completed) {
                    funnel.logAbandoned(title.getDisplayText());
                }
            });
            startExpanded();
        }

        static void shareAsText(@NonNull Context context, @NonNull PageTitle title,
                                @NonNull String selectedText, @Nullable ShareAFactFunnel funnel) {
            String introText = context.getString(R.string.snippet_share_intro,
                    title.getDisplayText(),
                    UriUtil.getUrlWithProvenance(context, title, R.string.prov_share_text));
            ShareUtil.shareText(context, title.getDisplayText(),
                    constructShareText(selectedText, introText));
            if (funnel != null) {
                funnel.logShareIntent(selectedText, ShareMode.text);
            }
        }

        private static String constructShareText(String selectedText, String introText) {
            return selectedText + "\n\n" + introText;
        }
    }
}
