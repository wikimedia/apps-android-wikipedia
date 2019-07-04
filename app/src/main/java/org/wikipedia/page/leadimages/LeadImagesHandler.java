package org.wikipedia.page.leadimages;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.bridge.CommunicationBridge;
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
import org.wikipedia.settings.Prefs;
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

import static org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class LeadImagesHandler {
    /**
     * Minimum screen height for enabling lead images. If the screen is smaller than
     * this height, lead images will not be displayed, and will be substituted with just
     * the page title.
     */
    private static final int MIN_SCREEN_HEIGHT_DP = 480;
    private static final String COMMONS_IMAGE_URL = "/wikipedia/commons/";

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete(int sequence);
    }

    @NonNull private final PageFragment parentFragment;
    @NonNull private final CommunicationBridge bridge;

    @NonNull private final PageHeaderView pageHeaderView;

    private int displayHeightDp;
    private SuggestedEditsSummary sourceSummary, targetSummary;
    private boolean isTranslation;
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

    private void setWebViewPaddingTop() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingTop", isMainPage()
                    ? Math.round(getContentTopOffsetPx(getActivity()) / DimenUtil.getDensityScalar())
                    : Math.round(pageHeaderView.getHeight() / DimenUtil.getDensityScalar()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingTop", payload);
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
        if ((!Prefs.isSuggestedEditsAddCaptionsUnlocked() && !Prefs.isSuggestedEditsTranslateCaptionsUnlocked()) || !getLeadImageUrl().contains(COMMONS_IMAGE_URL) || getPage() == null) {
            pageHeaderView.setUpCallToAction(null);
            return;
        }
        WikipediaApp app = WikipediaApp.getInstance();

        disposables.add(ServiceFactory.getRest(getTitle().getWikiSite()).getMedia(getTitle().getConvertedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(gallery -> {
                    GalleryItem galleryItem = null;
                    List<GalleryItem> list = gallery.getItems("image");
                    for (GalleryItem item : list) {
                        if (getPage() != null && item.getFilePage().contains(getPage().getPageProperties().getLeadImageName())) {
                            galleryItem = item;
                            if (TextUtils.isEmpty(item.getStructuredCaptions().get(getTitle().getWikiSite().languageCode()))) {
                                pageHeaderView.setUpCallToAction(app.getResources().getString(R.string.suggested_edits_article_cta_add_image_caption));
                                sourceSummary = new SuggestedEditsSummary(getTitle().getPrefixedText(), app.getAppOrSystemLanguageCode(), getTitle(),
                                        getTitle().getDisplayText(), getTitle().getDisplayText(), StringUtil.fromHtml(item.getDescription().getHtml()).toString(), item.getThumbnailUrl(), item.getPreferredSizedImageUrl(),
                                        null, null, null, null);

                                return;
                            }
                        }
                    }
                    if (galleryItem != null) {
                        String title = galleryItem.getFilePage().equals(Service.COMMONS_URL) ? galleryItem.getTitles().getCanonical() : UriUtil.getTitleFromUrl(galleryItem.getFilePage());

                        if (app.language().getAppLanguageCodes().size() > 1 && !Prefs.isSuggestedEditsTranslateCaptionsUnlocked()) {
                            disposables.add(MediaHelper.INSTANCE.getImageCaptions(title)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(captions -> {
                                                for (String lang : app.language().getAppLanguageCodes()) {
                                                    if (!captions.containsKey(lang)) {
                                                        isTranslation = true;
                                                        PageTitle sourceTitle = new PageTitle(getTitle().getText(), new WikiSite(Service.COMMONS_URL, app.language().getAppLanguageCodes().get(0)));
                                                        PageTitle targetTitle = new PageTitle(getTitle().getText(), new WikiSite(Service.COMMONS_URL, lang));
                                                        sourceSummary = new SuggestedEditsSummary(sourceTitle.getPrefixedText(), sourceTitle.getWikiSite().languageCode(), sourceTitle,
                                                                sourceTitle.getDisplayText(), sourceTitle.getDisplayText(), null, getLeadImageUrl(), getLeadImageUrl(),
                                                                null, null, null, null);

                                                        targetSummary = new SuggestedEditsSummary(targetTitle.getPrefixedText(), targetTitle.getWikiSite().languageCode(), targetTitle,
                                                                targetTitle.getDisplayText(), targetTitle.getDisplayText(), null, getLeadImageUrl(), getLeadImageUrl(),
                                                                null, null, null, null);
                                                        pageHeaderView.setUpCallToAction(String.format(app.getResources().getString(R.string.suggested_edits_article_cta_translate_image_caption), app.language().getAppLanguageLocalizedName(lang)));
                                                        break;
                                                    }
                                                }
                                            },
                                            L::e));
                        }
                    }
                }, L::e));
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
            public void onCallToActionContainerClicked() {
                getActivity().startActivityForResult(DescriptionEditActivity.newIntent(getActivity(), getTitle(), null, sourceSummary, targetSummary, isTranslation ? SUGGESTED_EDITS_TRANSLATE_CAPTION : SUGGESTED_EDITS_ADD_CAPTION),
                        ACTIVITY_REQUEST_DESCRIPTION_EDIT);
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
        if (disposables != null && !disposables.isDisposed()) {
            disposables.clear();
        }
    }
}
