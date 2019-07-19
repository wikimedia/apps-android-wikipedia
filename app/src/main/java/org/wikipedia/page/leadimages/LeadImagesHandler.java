package org.wikipedia.page.leadimages;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.media.MediaHelper;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.gallery.GalleryItem;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.suggestededits.SuggestedEditsSummary;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;
import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class LeadImagesHandler {
    /**
     * Minimum screen height for enabling lead images. If the screen is smaller than
     * this height, lead images will not be displayed, and will be substituted with just
     * the page title.
     */
    private static final int MIN_SCREEN_HEIGHT_DP = 480;
    private static final String URL_FRAGMENT_FROM_COMMONS = "/wikipedia/commons/";

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete(int sequence);
    }

    @NonNull private final PageFragment parentFragment;
    @NonNull private final CommunicationBridge bridge;

    @NonNull private final PageHeaderView pageHeaderView;

    private int displayHeightDp;

    @Nullable private SuggestedEditsSummary callToActionSourceSummary;
    @Nullable private SuggestedEditsSummary callToActionTargetSummary;
    private boolean callToActionIsTranslation;
    private CompositeDisposable disposables = new CompositeDisposable();

    public LeadImagesHandler(@NonNull final PageFragment parentFragment,
                             @NonNull CommunicationBridge bridge,
                             @NonNull ObservableWebView webView,
                             @NonNull PageHeaderView pageHeaderView) {
        this.parentFragment = parentFragment;
        this.pageHeaderView = pageHeaderView;
        this.pageHeaderView.setWebView(webView);

        this.bridge = bridge;
        webView.addOnScrollChangeListener(pageHeaderView);

        initDisplayDimensions();
        initArticleHeaderView();
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        pageHeaderView.hide();
    }

    @Nullable public Bitmap getLeadImageBitmap() {
        return isLeadImageEnabled() ? pageHeaderView.copyBitmap() : null;
    }

    public boolean isLeadImageEnabled() {
        return isImageDownloadEnabled()
                && displayHeightDp >= MIN_SCREEN_HEIGHT_DP
                && !isMainPage()
                && !TextUtils.isEmpty(getLeadImageUrl());
    }

    /**
     * Triggers a chain of events that will lay out the lead image, page title, and other
     * elements, at the end of which the WebView contents may begin to be composed.
     * These events (performed asynchronously) are in the following order:
     * - Dynamically resize the page title TextView and, if necessary, adjust its font size
     * based on the length of our page title.
     * - Dynamically resize the lead image container view and restyle it, if necessary, depending
     * on whether the page contains a lead image, and whether our screen resolution is high
     * enough to warrant a lead image.
     * - Send a "padding" event to the WebView so that any subsequent content that's added to it
     * will be correctly offset to account for the lead image view (or lack of it)
     * - Make the lead image view visible.
     * - Fire a callback to the provided Listener indicating that the rest of the WebView content
     * can now be loaded.
     * - Fetch and display the WikiData description for this page, if available.
     * <p/>
     * Realistically, the whole process will happen very quickly, and almost unnoticeably to the
     * user. But it still needs to be asynchronous because we're dynamically laying out views, and
     * because the padding "event" that we send to the WebView must come before any other content
     * is sent to it, so that the effect doesn't look jarring to the user.
     *
     * @param listener Listener that will receive an event when the layout is completed.
     */
    public void beginLayout(OnLeadImageLayoutListener listener,
                            int sequence) {
        if (getPage() == null) {
            return;
        }
        initDisplayDimensions();
        loadLeadImage();
        layoutViews(listener, sequence);
    }

    /**
     * The final step in the layout process:
     * Apply sizing and styling to our page title and lead image views, based on how large our
     * page title ended up, and whether we should display the lead image.
     *
     * @param listener Listener that will receive an event when the layout is completed.
     */
    private void layoutViews(OnLeadImageLayoutListener listener, int sequence) {
        if (!isFragmentAdded()) {
            return;
        }

        if (isMainPage()) {
            pageHeaderView.hide();
            // explicitly set WebView padding, since onLayoutChange will not be called.
            setWebViewPaddingTop();
        } else {
            pageHeaderView.show(isLeadImageEnabled());
        }

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete(sequence);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void setWebViewPaddingTop() {
        int topPadding = isMainPage()
                ? Math.round(getContentTopOffsetPx(getActivity()) / DimenUtil.getDensityScalar())
                : Math.round(pageHeaderView.getHeight() / DimenUtil.getDensityScalar());
        parentFragment.getWebView().evaluateJavascript(JavaScriptActionHandler.setMargin(getActivity(), topPadding + 16, 16, 0, 16), null);
    }

    /**
     * Determines and sets displayHeightDp for the lead images layout.
     */
    private void initDisplayDimensions() {
        displayHeightDp = (int) (DimenUtil.getDisplayHeightPx() / DimenUtil.getDensityScalar());
    }

    private void loadLeadImage() {
        String leadImageUrl = getLeadImageUrl();
        if (leadImageUrl == null) {
            loadLeadImage(null);
        } else {
            loadLeadImage(leadImageUrl);
        }
    }

    private void loadLeadImage(@Nullable String url) {
        if (!isMainPage() && !TextUtils.isEmpty(url) && isLeadImageEnabled()) {
            pageHeaderView.show(isLeadImageEnabled());
            pageHeaderView.loadImage(url);
            updateCallToAction();
        } else {
            pageHeaderView.loadImage(null);
        }
    }

    private void updateCallToAction() {
        dispose();
        pageHeaderView.setUpCallToAction(null);
        if (!AccountUtil.isLoggedIn() || getLeadImageUrl() == null || !getLeadImageUrl().contains(URL_FRAGMENT_FROM_COMMONS) || getPage() == null) {
            return;
        }
        GalleryItem[] galleryItem = {null};
        String[] title = {null};
        disposables.add(ServiceFactory.getRest(getTitle().getWikiSite()).getMedia(getTitle().getConvertedText())
                .flatMap(gallery -> {
                    List<GalleryItem> list = gallery.getItems("image");
                    for (GalleryItem item : list) {
                        if (getPage() != null && item.getFilePage().contains(StringUtil.addUnderscores(getPage().getPageProperties().getLeadImageName()))) {
                            galleryItem[0] = item;
                            title[0] = item.getFilePage().equals(Service.COMMONS_URL) ? item.getTitles().getCanonical() : UriUtil.getTitleFromUrl(item.getFilePage());
                            return MediaHelper.INSTANCE.getImageCaptions(title[0]);
                        }
                    }
                    return null;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(captions -> {
                            PageTitle captionSourcePageTitle, captionTargetPageTitle;
                            if (galleryItem[0] != null) {
                                WikipediaApp app = WikipediaApp.getInstance();
                                captionSourcePageTitle = new PageTitle(title[0], new WikiSite(Service.COMMONS_URL, getTitle().getWikiSite().languageCode()));

                                if (!captions.containsKey(getTitle().getWikiSite().languageCode())) {
                                    pageHeaderView.setUpCallToAction(app.getResources().getString(R.string.suggested_edits_article_cta_image_caption, app.language().getAppLanguageLocalizedName(getTitle().getWikiSite().languageCode())));
                                    callToActionSourceSummary = new SuggestedEditsSummary(captionSourcePageTitle.getPrefixedText(), getTitle().getWikiSite().languageCode(), captionSourcePageTitle,
                                            captionSourcePageTitle.getDisplayText(), captionSourcePageTitle.getDisplayText(), StringUtils.defaultIfBlank(StringUtil.fromHtml(galleryItem[0].getDescription().getHtml()).toString(), getActivity().getString(R.string.suggested_edits_no_description)),
                                            galleryItem[0].getThumbnailUrl(), null, null, null, null);

                                    return;
                                }
                                if (app.language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                                    for (String lang : app.language().getAppLanguageCodes()) {
                                        if (!captions.containsKey(lang)) {
                                            callToActionIsTranslation = true;
                                            captionTargetPageTitle = new PageTitle(title[0], new WikiSite(Service.COMMONS_URL, lang));
                                            String currentCaption = captions.get(getTitle().getWikiSite().languageCode());
                                            captionSourcePageTitle.setDescription(currentCaption);
                                            callToActionSourceSummary = new SuggestedEditsSummary(captionSourcePageTitle.getPrefixedText(), captionSourcePageTitle.getWikiSite().languageCode(), captionSourcePageTitle,
                                                    captionSourcePageTitle.getDisplayText(), captionSourcePageTitle.getDisplayText(), currentCaption, getLeadImageUrl(),
                                                    null, null, null, null);

                                            callToActionTargetSummary = new SuggestedEditsSummary(captionTargetPageTitle.getPrefixedText(), captionTargetPageTitle.getWikiSite().languageCode(), captionTargetPageTitle,
                                                    captionTargetPageTitle.getDisplayText(), captionTargetPageTitle.getDisplayText(), null, getLeadImageUrl(),
                                                    null, null, null, null);
                                            pageHeaderView.setUpCallToAction(app.getResources().getString(R.string.suggested_edits_article_cta_image_caption, app.language().getAppLanguageLocalizedName(lang)));
                                            break;
                                        }
                                    }
                                }
                            }
                        },
                        L::e));
    }

    @Nullable private String getLeadImageUrl() {
        String url = getPage() == null ? null : getPage().getPageProperties().getLeadImageUrl();
        if (url == null) {
            return null;
        }

        // Conditionally add the PageTitle's URL scheme and authority if these are missing from the
        // PageProperties' URL.
        Uri fullUri = Uri.parse(url);
        String scheme = getTitle().getWikiSite().scheme();
        String authority = getTitle().getWikiSite().authority();

        if (fullUri.getScheme() != null) {
            scheme = fullUri.getScheme();
        }
        if (fullUri.getAuthority() != null) {
            authority = fullUri.getAuthority();
        }
        return new Uri.Builder()
                .scheme(scheme)
                .authority(authority)
                .path(fullUri.getPath())
                .toString();
    }

    private void initArticleHeaderView() {
        pageHeaderView.addOnLayoutChangeListener((View v, int left, int top, int right, int bottom,
                                                  int oldLeft, int oldTop, int oldRight, int oldBottom) -> setWebViewPaddingTop());
        pageHeaderView.setCallback(new PageHeaderView.Callback() {
            @Override
            public void onImageClicked() {
                if (getPage() != null && isLeadImageEnabled()) {
                    String imageName = getPage().getPageProperties().getLeadImageName();
                    String imageUrl = getPage().getPageProperties().getLeadImageUrl();
                    if (imageName != null && imageUrl != null) {
                        String filename = "File:" + imageName;
                        WikiSite wiki = getTitle().getWikiSite();
                        getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                                parentFragment.getTitleOriginal(), filename, UriUtil.resolveProtocolRelativeUrl(imageUrl), wiki,
                                GalleryFunnel.SOURCE_LEAD_IMAGE),
                                Constants.ACTIVITY_REQUEST_GALLERY);
                    }
                }
            }

            @Override
            public void onCallToActionClicked() {
                if (callToActionIsTranslation ? (callToActionTargetSummary != null && callToActionSourceSummary != null) : callToActionSourceSummary != null) {
                    getActivity().startActivityForResult(DescriptionEditActivity.newIntent(getActivity(),
                            callToActionIsTranslation ? callToActionTargetSummary.getPageTitle() : callToActionSourceSummary.getPageTitle(), null,
                            callToActionSourceSummary, callToActionTargetSummary, callToActionIsTranslation ? SUGGESTED_EDITS_TRANSLATE_CAPTION : SUGGESTED_EDITS_ADD_CAPTION),
                            ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT);
                }
            }
        });
    }

    private boolean isMainPage() {
        return getPage() != null && getPage().isMainPage();
    }

    private PageTitle getTitle() {
        return parentFragment.getTitle();
    }

    @Nullable
    private Page getPage() {
        return parentFragment.getPage();
    }

    private boolean isFragmentAdded() {
        return parentFragment.isAdded();
    }

    private FragmentActivity getActivity() {
        return parentFragment.getActivity();
    }

    public void dispose() {
        disposables.clear();
        callToActionSourceSummary = null;
        callToActionTargetSummary = null;
        callToActionIsTranslation = false;
    }

    @Nullable public String getCallToActionEditLang() {
        if (callToActionIsTranslation ? (callToActionTargetSummary == null || callToActionSourceSummary == null) : callToActionSourceSummary == null) {
            return null;
        }
        return callToActionIsTranslation ? callToActionTargetSummary.getPageTitle().getWikiSite().languageCode()
                : callToActionSourceSummary.getPageTitle().getWikiSite().languageCode();
    }
}
