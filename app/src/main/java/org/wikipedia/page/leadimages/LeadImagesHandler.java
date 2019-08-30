package org.wikipedia.page.leadimages;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.media.MediaHelper;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.suggestededits.SuggestedEditsSummary;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION;
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
                && !(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                && displayHeightDp >= MIN_SCREEN_HEIGHT_DP
                && !isMainPage()
                && !TextUtils.isEmpty(getLeadImageUrl());
    }

    public int getPaddingTop() {
        return isLeadImageEnabled() ? Math.round(leadImageHeightForDevice() / DimenUtil.getDensityScalar())
                : Math.round(parentFragment.requireActivity().getResources().getDimensionPixelSize(R.dimen.lead_no_image_top_offset_dp) / DimenUtil.getDensityScalar());
    }

    public void setWebViewPaddingTop() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingTop", getPaddingTop());
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

    public void loadLeadImage() {
        String url = getLeadImageUrl();
        if (getPage() == null) {
            return;
        }
        initDisplayDimensions();
        if (!isMainPage() && !TextUtils.isEmpty(url) && isLeadImageEnabled()) {
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
        disposables.add(Observable.zip(MediaHelper.INSTANCE.getImageCaptions(imageTitle),
                ServiceFactory.get(getTitle().getWikiSite()).getImageExtMetadata(imageTitle), Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                            WikipediaApp app = WikipediaApp.getInstance();
                            PageTitle captionSourcePageTitle = new PageTitle(imageTitle, new WikiSite(Service.COMMONS_URL, getTitle().getWikiSite().languageCode()));
                            ImageInfo imageInfo = pair.second.query().firstPage().imageInfo();

                            if (!pair.first.containsKey(getTitle().getWikiSite().languageCode())) {
                                pageHeaderView.setUpCallToAction(app.getResources().getString(R.string.suggested_edits_article_cta_image_caption, app.language().getAppLanguageLocalizedName(getTitle().getWikiSite().languageCode())));
                                callToActionSourceSummary = new SuggestedEditsSummary(captionSourcePageTitle.getPrefixedText(), getTitle().getWikiSite().languageCode(), captionSourcePageTitle,
                                        captionSourcePageTitle.getDisplayText(), captionSourcePageTitle.getDisplayText(), StringUtils.defaultIfBlank(StringUtil.fromHtml(imageInfo.getMetadata().imageDescription()).toString(), null),
                                        imageInfo.getThumbUrl(), null, null, null, null);

                                return;
                            }
                            if (app.language().getAppLanguageCodes().size() >= MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                                for (String lang : app.language().getAppLanguageCodes()) {
                                    if (!pair.first.containsKey(lang)) {
                                        callToActionIsTranslation = true;
                                        PageTitle captionTargetPageTitle = new PageTitle(imageTitle, new WikiSite(Service.COMMONS_URL, lang));
                                        String currentCaption = pair.first.get(getTitle().getWikiSite().languageCode());
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
        pageHeaderView.setCallback(new PageHeaderView.Callback() {
            @Override
            public void onImageClicked() {
                openImageInGallery(null);
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

    public void openImageInGallery(@Nullable  String language) {
        if (getPage() != null && isLeadImageEnabled()) {
            String imageName = getPage().getPageProperties().getLeadImageName();
            if (imageName != null) {
                String filename = "File:" + imageName;
                WikiSite wiki = language == null ? getTitle().getWikiSite() : WikiSite.forLanguageCode(language);
                getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                        parentFragment.getTitleOriginal(), filename, wiki,
                        GalleryFunnel.SOURCE_LEAD_IMAGE),
                        Constants.ACTIVITY_REQUEST_GALLERY);
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
