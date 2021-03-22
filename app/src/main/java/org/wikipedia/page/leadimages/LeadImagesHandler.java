package org.wikipedia.page.leadimages;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentActivity;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.Constants.ImageEditType;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.commons.ImageTagsProvider;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.media.MediaHelper;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.suggestededits.PageSummaryForEdit;
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ObservableWebView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_IMAGE_TAGS_EDIT;
import static org.wikipedia.Constants.InvokeSource.LEAD_IMAGE;
import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;
import static org.wikipedia.util.DimenUtil.leadImageHeightForDevice;

public class LeadImagesHandler {
    /**
     * Minimum screen height for enabling lead images. If the screen is smaller than
     * this height, lead images will not be displayed, and will be substituted with just
     * the page title.
     */
    private static final int MIN_SCREEN_HEIGHT_DP = 480;

    @NonNull private final PageFragment parentFragment;

    @NonNull private final PageHeaderView pageHeaderView;

    private int displayHeightDp;

    @Nullable private PageSummaryForEdit callToActionSourceSummary;
    @Nullable private PageSummaryForEdit callToActionTargetSummary;
    private boolean callToActionIsTranslation;
    private ImageEditType imageEditType;
    private PageTitle captionSourcePageTitle;
    private PageTitle captionTargetPageTitle;
    private MwQueryPage imagePage;
    private CompositeDisposable disposables = new CompositeDisposable();

    public LeadImagesHandler(@NonNull final PageFragment parentFragment,
                             @NonNull ObservableWebView webView,
                             @NonNull PageHeaderView pageHeaderView) {
        this.parentFragment = parentFragment;
        this.pageHeaderView = pageHeaderView;
        this.pageHeaderView.setWebView(webView);

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

    public boolean isLeadImageEnabled() {
        return isImageDownloadEnabled()
                && !(DimenUtil.isLandscape(getActivity()))
                && displayHeightDp >= MIN_SCREEN_HEIGHT_DP
                && !isMainPage()
                && !TextUtils.isEmpty(getLeadImageUrl());
    }

    public int getTopMargin() {
        return DimenUtil.roundedPxToDp(isLeadImageEnabled() ? leadImageHeightForDevice(parentFragment.requireContext())
                : parentFragment.getToolbarMargin());
    }

    /**
     * Determines and sets displayHeightDp for the lead images layout.
     */
    private void initDisplayDimensions() {
        displayHeightDp = (int) (DimenUtil.getDisplayHeightPx() / DimenUtil.getDensityScalar());
    }

    public void loadLeadImage() {
        String url = getLeadImageUrl();
        initDisplayDimensions();
        if (getPage() != null && !isMainPage() && !TextUtils.isEmpty(url) && isLeadImageEnabled()) {
            pageHeaderView.show();
            pageHeaderView.loadImage(url);
            updateCallToAction();
        } else {
            pageHeaderView.loadImage(null);
        }
    }

    private void updateCallToAction() {
        dispose();
        pageHeaderView.setUpCallToAction(null);
        if (!AccountUtil.isLoggedIn() || getLeadImageUrl() == null || !getLeadImageUrl().contains(Service.URL_FRAGMENT_FROM_COMMONS) || getPage() == null) {
            return;
        }
        String imageTitle = "File:" + getPage().getPageProperties().getLeadImageName();
        disposables.add(ServiceFactory.get(new WikiSite(Service.COMMONS_URL)).getProtectionInfo(imageTitle)
                .subscribeOn(Schedulers.io())
                .map(response -> response.query().isEditProtected())
                .flatMap(isProtected -> isProtected ? Observable.empty() : Observable.zip(MediaHelper.INSTANCE.getImageCaptions(imageTitle),
                        ServiceFactory.get(new WikiSite(Service.COMMONS_URL)).getImageInfo(imageTitle, WikipediaApp.getInstance().getAppOrSystemLanguageCode()), Pair::new))
                .flatMap(pair -> {
                            captionSourcePageTitle = new PageTitle(imageTitle, new WikiSite(Service.COMMONS_URL, getTitle().getWikiSite().languageCode()));
                            captionSourcePageTitle.setDescription(pair.first.get(getTitle().getWikiSite().languageCode()));
                            imagePage = pair.second.query().firstPage();
                            imageEditType = null; // Need to clear value from precious call
                            if (!pair.first.containsKey(getTitle().getWikiSite().languageCode())) {
                                imageEditType = ImageEditType.ADD_CAPTION;
                                return ImageTagsProvider.getImageTagsObservable(pair.second.query().firstPage().pageId(), getTitle().getWikiSite().languageCode());
                            }
                            if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                                for (String lang : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
                                    if (!pair.first.containsKey(lang)) {
                                        imageEditType = ImageEditType.ADD_CAPTION_TRANSLATION;
                                        captionTargetPageTitle = new PageTitle(imageTitle, new WikiSite(Service.COMMONS_URL, lang));
                                        break;
                                    }
                                }
                            }
                            return ImageTagsProvider.getImageTagsObservable(pair.second.query().firstPage().pageId(), getTitle().getWikiSite().languageCode());
                        }
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(imageTagsResult -> {
                    if (imageEditType != ImageEditType.ADD_CAPTION && imageTagsResult != null && imageTagsResult.size() == 0) {
                        imageEditType = ImageEditType.ADD_TAGS;
                    }
                    finalizeCallToAction();
                })
        );
    }

    private void finalizeCallToAction() {
        switch (imageEditType) {
            case ADD_TAGS:
                pageHeaderView.setUpCallToAction(parentFragment.getString(R.string.suggested_edits_article_cta_image_tags));
                break;
            case ADD_CAPTION_TRANSLATION:
                callToActionIsTranslation = true;
                callToActionSourceSummary = new PageSummaryForEdit(captionSourcePageTitle.getPrefixedText(), captionSourcePageTitle.getWikiSite().languageCode(), captionSourcePageTitle,
                        captionSourcePageTitle.getDisplayText(), captionSourcePageTitle.getDescription(), getLeadImageUrl());

                callToActionTargetSummary = new PageSummaryForEdit(captionTargetPageTitle.getPrefixedText(), captionTargetPageTitle.getWikiSite().languageCode(), captionTargetPageTitle,
                        captionTargetPageTitle.getDisplayText(), null, getLeadImageUrl());
                pageHeaderView.setUpCallToAction(parentFragment.getString(R.string.suggested_edits_article_cta_image_caption_in_language, WikipediaApp.getInstance().language().getAppLanguageLocalizedName(captionTargetPageTitle.getWikiSite().languageCode())));
                break;
            default:
                callToActionSourceSummary = new PageSummaryForEdit(captionSourcePageTitle.getPrefixedText(), getTitle().getWikiSite().languageCode(), captionSourcePageTitle,
                        captionSourcePageTitle.getDisplayText(), StringUtils.defaultIfBlank(StringUtil.fromHtml(imagePage.imageInfo().getMetadata().imageDescription()).toString(), null),
                        imagePage.imageInfo().getThumbUrl());
                pageHeaderView.setUpCallToAction(parentFragment.getString(R.string.suggested_edits_article_cta_image_caption));
        }
    }

    private int getLeadImageWidth() {
        return getPage() == null ? pageHeaderView.image.getWidth() : getPage().getPageProperties().getLeadImageWidth();
    }

    private int getLeadImageHeight() {
        return getPage() == null ? pageHeaderView.image.getHeight() : getPage().getPageProperties().getLeadImageHeight();
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
        pageHeaderView.setCallback(new PageHeaderView.Callback() {
            @Override
            public void onImageClicked() {
                openImageInGallery(null);
            }

            @Override
            public void onCallToActionClicked() {
                if (imageEditType == ImageEditType.ADD_TAGS) {
                    getActivity().startActivityForResult(SuggestedEditsImageTagEditActivity.Companion.newIntent(getActivity(), imagePage, LEAD_IMAGE), ACTIVITY_REQUEST_IMAGE_TAGS_EDIT);
                    return;
                }
                if (imageEditType == ImageEditType.ADD_CAPTION_TRANSLATION ? (callToActionTargetSummary != null && callToActionSourceSummary != null) : callToActionSourceSummary != null) {
                    getActivity().startActivityForResult(DescriptionEditActivity.newIntent(getActivity(),
                            callToActionIsTranslation ? callToActionTargetSummary.getPageTitle() : callToActionSourceSummary.getPageTitle(), null,
                            callToActionSourceSummary, callToActionTargetSummary, callToActionIsTranslation ? TRANSLATE_CAPTION : ADD_CAPTION, LEAD_IMAGE),
                            ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT);
                }
            }
        });
    }

    public void openImageInGallery(@Nullable String language) {
        if (getPage() != null && isLeadImageEnabled()) {
            String imageName = getPage().getPageProperties().getLeadImageName();
            if (imageName != null) {
                String filename = "File:" + imageName;
                WikiSite wiki = language == null ? getTitle().getWikiSite() : WikiSite.forLanguageCode(language);

                JavaScriptActionHandler.ImageHitInfo hitInfo = new JavaScriptActionHandler.ImageHitInfo(pageHeaderView.image.getLeft(),
                        pageHeaderView.image.getTop(), getLeadImageWidth(), getLeadImageHeight(),
                        getLeadImageUrl(), true);

                GalleryActivity.setTransitionInfo(hitInfo);

                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(getActivity(), pageHeaderView.image, getActivity().getString(R.string.transition_page_gallery));

                getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                        parentFragment.getTitle(), filename, wiki, parentFragment.getRevision(),
                        GalleryFunnel.SOURCE_LEAD_IMAGE),
                        Constants.ACTIVITY_REQUEST_GALLERY, options.toBundle());
            }
        }
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
