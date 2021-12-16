package org.wikipedia.page.leadimages

import android.net.Uri
import androidx.core.app.ActivityOptionsCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.ImageEditType
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.commons.ImageTagsProvider
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.mwapi.media.MediaHelper
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ObservableWebView

class LeadImagesHandler(private val parentFragment: PageFragment,
                        webView: ObservableWebView,
                        private val pageHeaderView: PageHeaderView) {
    private var displayHeightDp = 0
    private var callToActionSourceSummary: PageSummaryForEdit? = null
    private var callToActionTargetSummary: PageSummaryForEdit? = null
    private var callToActionIsTranslation = false
    private var imageEditType: ImageEditType? = null
    private var captionSourcePageTitle: PageTitle? = null
    private var captionTargetPageTitle: PageTitle? = null
    private var imagePage: MwQueryPage? = null
    private val isMainPage get() = page?.run { isMainPage } ?: false
    private val title get() = parentFragment.title
    private val page get() = parentFragment.page
    private val activity get() = parentFragment.requireActivity()
    private val disposables = CompositeDisposable()

    private val isLeadImageEnabled get() = Prefs.isImageDownloadEnabled && !DimenUtil.isLandscape(activity) && displayHeightDp >= MIN_SCREEN_HEIGHT_DP && !isMainPage && !leadImageUrl.isNullOrEmpty()
    private val leadImageWidth get() = page?.run { pageProperties.leadImageWidth } ?: pageHeaderView.imageView.width
    private val leadImageHeight get() = page?.run { pageProperties.leadImageHeight } ?: pageHeaderView.imageView.height

    // Conditionally add the PageTitle's URL scheme and authority if these are missing from the
    // PageProperties' URL.
    private val leadImageUrl: String?
        get() {
            val url = page?.run { pageProperties.leadImageUrl } ?: return null
            title?.let {
                // Conditionally add the PageTitle's URL scheme and authority if these are missing from the
                // PageProperties' URL.
                val fullUri = Uri.parse(url)
                var scheme: String? = it.wikiSite.scheme()
                var authority: String? = it.wikiSite.authority()
                if (fullUri.scheme != null) {
                    scheme = fullUri.scheme
                }
                if (fullUri.authority != null) {
                    authority = fullUri.authority
                }
                return Uri.Builder()
                    .scheme(scheme)
                    .authority(authority)
                    .path(fullUri.path)
                    .toString()
            } ?: return null
        }

    val topMargin get() = DimenUtil.roundedPxToDp((if (isLeadImageEnabled) DimenUtil.leadImageHeightForDevice(parentFragment.requireContext()) else parentFragment.toolbarMargin.toFloat()).toFloat())
    val callToActionEditLang get() =
        if (callToActionIsTranslation) callToActionTargetSummary?.pageTitle?.wikiSite?.languageCode else callToActionSourceSummary?.pageTitle?.wikiSite?.languageCode

    init {
        pageHeaderView.setWebView(webView)
        webView.addOnScrollChangeListener(pageHeaderView)
        initDisplayDimensions()
        initArticleHeaderView()
    }

    private fun initDisplayDimensions() {
        displayHeightDp = (DimenUtil.displayHeightPx / DimenUtil.densityScalar).toInt()
    }

    private fun updateCallToAction() {
        dispose()
        pageHeaderView.setUpCallToAction(null)
        if (!AccountUtil.isLoggedIn || leadImageUrl == null || !leadImageUrl!!.contains(Service.URL_FRAGMENT_FROM_COMMONS) || page == null) {
            return
        }
        title?.let {
            val imageTitle = "File:" + page!!.pageProperties.leadImageName
            disposables.add(ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getProtectionInfo(imageTitle)
                .subscribeOn(Schedulers.io())
                .map { response -> response.query?.isEditProtected ?: false }
                .flatMap { isProtected ->
                    if (isProtected) Observable.empty() else Observable.zip(MediaHelper.getImageCaptions(imageTitle),
                        ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getImageInfo(imageTitle, WikipediaApp.getInstance().appOrSystemLanguageCode), { first, second -> Pair(first, second) })
                }
                .flatMap { pair ->
                    captionSourcePageTitle = PageTitle(imageTitle, WikiSite(Service.COMMONS_URL, it.wikiSite.languageCode))
                    captionSourcePageTitle!!.description = pair.first[it.wikiSite.languageCode]
                    imagePage = pair.second.query?.firstPage()
                    imageEditType = null // Need to clear value from precious call
                    if (!pair.first.containsKey(it.wikiSite.languageCode)) {
                        imageEditType = ImageEditType.ADD_CAPTION
                        return@flatMap ImageTagsProvider.getImageTagsObservable(pair.second.query?.firstPage()!!.pageId, it.wikiSite.languageCode)
                    }
                    if (WikipediaApp.getInstance().language().appLanguageCodes.size >= Constants.MIN_LANGUAGES_TO_UNLOCK_TRANSLATION) {
                        for (lang in WikipediaApp.getInstance().language().appLanguageCodes) {
                            if (!pair.first.containsKey(lang)) {
                                imageEditType = ImageEditType.ADD_CAPTION_TRANSLATION
                                captionTargetPageTitle = PageTitle(imageTitle, WikiSite(Service.COMMONS_URL, lang!!))
                                break
                            }
                        }
                    }
                    ImageTagsProvider.getImageTagsObservable(pair.second.query?.firstPage()!!.pageId, it.wikiSite.languageCode)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { imageTagsResult ->
                    if (imageEditType != ImageEditType.ADD_CAPTION && imageTagsResult.isEmpty()) {
                        imageEditType = ImageEditType.ADD_TAGS
                    }
                    finalizeCallToAction()
                }
            )
            pageHeaderView.imageView.contentDescription = parentFragment.getString(R.string.image_content_description, it.displayText)
        }
    }

    private fun finalizeCallToAction() {
        if (imageEditType == null) {
            return
        }
        when (imageEditType) {
            ImageEditType.ADD_TAGS -> pageHeaderView.setUpCallToAction(parentFragment.getString(R.string.suggested_edits_article_cta_image_tags))
            ImageEditType.ADD_CAPTION_TRANSLATION -> {
                callToActionIsTranslation = true
                captionSourcePageTitle?.run {
                    callToActionSourceSummary = PageSummaryForEdit(prefixedText, wikiSite.languageCode, this, displayText, description, leadImageUrl)
                }
                captionTargetPageTitle?.run {
                    callToActionTargetSummary = PageSummaryForEdit(prefixedText, wikiSite.languageCode, this, displayText, null, leadImageUrl)
                    pageHeaderView.setUpCallToAction(parentFragment.getString(R.string.suggested_edits_article_cta_image_caption_in_language, WikipediaApp.getInstance().language().getAppLanguageLocalizedName(wikiSite.languageCode)))
                }
            }
            else -> {
                captionSourcePageTitle?.run {
                    title?.let {
                        callToActionSourceSummary = PageSummaryForEdit(prefixedText, it.wikiSite.languageCode, this, displayText, StringUtil.fromHtml(imagePage?.imageInfo()?.metadata?.imageDescription().orEmpty()).toString(), imagePage?.imageInfo()?.thumbUrl)
                        pageHeaderView.setUpCallToAction(parentFragment.getString(R.string.suggested_edits_article_cta_image_caption))
                    }
                }
            }
        }
    }

    private fun initArticleHeaderView() {
        pageHeaderView.callback = object : PageHeaderView.Callback {
            override fun onImageClicked() {
                openImageInGallery(null)
            }

            override fun onCallToActionClicked() {
                if (imageEditType == ImageEditType.ADD_TAGS) {
                    imagePage?.let {
                        activity.startActivityForResult(SuggestedEditsImageTagEditActivity.newIntent(activity, it, InvokeSource.LEAD_IMAGE), Constants.ACTIVITY_REQUEST_IMAGE_TAGS_EDIT)
                    }
                    return
                }
                if (imageEditType == ImageEditType.ADD_CAPTION ||
                        imageEditType == ImageEditType.ADD_CAPTION_TRANSLATION) {
                    callToActionSourceSummary?.let { source ->
                        if (callToActionIsTranslation) {
                            callToActionTargetSummary?.let { target ->
                                activity.startActivityForResult(DescriptionEditActivity.newIntent(activity, target.pageTitle, null,
                                        source, target, DescriptionEditActivity.Action.TRANSLATE_CAPTION, InvokeSource.LEAD_IMAGE), Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT)
                            }
                        } else {
                            activity.startActivityForResult(DescriptionEditActivity.newIntent(activity, source.pageTitle, null,
                                    source, null, DescriptionEditActivity.Action.ADD_CAPTION, InvokeSource.LEAD_IMAGE), Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT)
                        }
                    }
                }
            }
        }
    }

    fun hide() {
        pageHeaderView.hide()
    }

    fun loadLeadImage() {
        val url = leadImageUrl
        initDisplayDimensions()
        if (page != null && !isMainPage && !url.isNullOrEmpty() && isLeadImageEnabled) {
            pageHeaderView.show()
            pageHeaderView.loadImage(url)
            updateCallToAction()
        } else {
            pageHeaderView.loadImage(null)
        }
    }

    fun openImageInGallery(language: String?) {
        if (isLeadImageEnabled) {
            page?.pageProperties?.leadImageName?.let { imageName ->
                title?.let {
                    val filename = "File:$imageName"
                    val wiki = language?.run { WikiSite.forLanguageCode(this) } ?: it.wikiSite
                    val hitInfo = JavaScriptActionHandler.ImageHitInfo(pageHeaderView.imageView.left.toFloat(),
                        pageHeaderView.imageView.top.toFloat(), leadImageWidth.toFloat(), leadImageHeight.toFloat(),
                        leadImageUrl!!, true)
                    GalleryActivity.setTransitionInfo(hitInfo)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pageHeaderView.imageView, activity.getString(R.string.transition_page_gallery))
                    activity.startActivityForResult(GalleryActivity.newIntent(activity,
                        parentFragment.title, filename, wiki, parentFragment.revision, GalleryFunnel.SOURCE_LEAD_IMAGE),
                        Constants.ACTIVITY_REQUEST_GALLERY, options.toBundle())
                }
            }
        }
    }

    fun dispose() {
        disposables.clear()
        callToActionSourceSummary = null
        callToActionTargetSummary = null
        callToActionIsTranslation = false
    }

    companion object {
        private const val MIN_SCREEN_HEIGHT_DP = 480
    }
}
