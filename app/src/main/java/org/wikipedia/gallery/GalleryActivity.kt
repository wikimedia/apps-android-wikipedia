package org.wikipedia.gallery

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Pair
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import butterknife.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Function3
import io.reactivex.rxjava3.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants
import org.wikipedia.Constants.ImageEditType
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.auth.AccountUtil.isLoggedIn
import org.wikipedia.bridge.JavaScriptActionHandler.ImageHitInfo
import org.wikipedia.commons.FilePageActivity.Companion.newIntent
import org.wikipedia.commons.ImageTagsProvider.getImageTagsObservable
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.media.MediaHelper.getImageCaptions
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditActivity.Companion.newIntent
import org.wikipedia.gallery.GalleryItemFragment.Companion.newInstance
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.linkpreview.LinkPreviewDialog.Companion.newInstance
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity.Companion.newIntent
import org.wikipedia.suggestededits.SuggestedEditsSnackbars.OpenPageListener
import org.wikipedia.suggestededits.SuggestedEditsSnackbars.show
import org.wikipedia.theme.Theme
import org.wikipedia.util.ClipboardUtil.setPlainText
import org.wikipedia.util.DeviceUtil.setContextClickAsLongClick
import org.wikipedia.util.DimenUtil.displayHeightPx
import org.wikipedia.util.DimenUtil.displayWidthPx
import org.wikipedia.util.FeedbackUtil.showMessage
import org.wikipedia.util.FeedbackUtil.showMessageAsPlainText
import org.wikipedia.util.GradientUtil.getPowerGradient
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize
import org.wikipedia.util.ShareUtil.shareImage
import org.wikipedia.util.ShareUtil.shareText
import org.wikipedia.util.StringUtil.fromHtml
import org.wikipedia.util.StringUtil.removeNamespace
import org.wikipedia.util.StringUtil.strip
import org.wikipedia.util.UriUtil.handleExternalLink
import org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl
import org.wikipedia.util.log.L.v
import org.wikipedia.views.PositionAwareFragmentStateAdapter
import org.wikipedia.views.ViewAnimations.ensureTranslationY
import org.wikipedia.views.ViewUtil.loadImage
import org.wikipedia.views.WikiErrorView
import java.io.File
import java.util.*

class GalleryActivity : BaseActivity(), LinkPreviewDialog.Callback, GalleryItemFragment.Callback {
    private val app = WikipediaApp.getInstance()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var pageTitle: PageTitle? = null

    @JvmField
    @BindView(R.id.gallery_transition_receiver)
    var transitionReceiver: ImageView? = null

    @JvmField
    @BindView(R.id.gallery_toolbar_container)
    var toolbarContainer: ViewGroup? = null

    @JvmField
    @BindView(R.id.gallery_toolbar)
    var toolbar: Toolbar? = null

    @JvmField
    @BindView(R.id.gallery_toolbar_gradient)
    var toolbarGradient: View? = null

    @JvmField
    @BindView(R.id.gallery_info_container)
    var infoContainer: ViewGroup? = null

    @JvmField
    @BindView(R.id.gallery_info_gradient)
    var infoGradient: View? = null

    @JvmField
    @BindView(R.id.gallery_progressbar)
    var progressBar: ProgressBar? = null

    @JvmField
    @BindView(R.id.gallery_description_container)
    var galleryDescriptionContainer: View? = null

    @JvmField
    @BindView(R.id.gallery_description_text)
    var descriptionText: TextView? = null

    @JvmField
    @BindView(R.id.gallery_license_container)
    var licenseContainer: View? = null

    @JvmField
    @BindView(R.id.gallery_license_icon)
    var licenseIcon: ImageView? = null

    @JvmField
    @BindView(R.id.gallery_license_icon_by)
    var byIcon: ImageView? = null

    @JvmField
    @BindView(R.id.gallery_license_icon_sa)
    var saIcon: ImageView? = null

    @JvmField
    @BindView(R.id.gallery_credit_text)
    var creditText: TextView? = null

    @JvmField
    @BindView(R.id.gallery_item_pager)
    var galleryPager: ViewPager2? = null

    @JvmField
    @BindView(R.id.view_gallery_error)
    var errorView: WikiErrorView? = null

    @JvmField
    @BindView(R.id.gallery_caption_edit_button)
    var captionEditButton: ImageView? = null

    @JvmField
    @BindView(R.id.gallery_cta_container)
    var ctaContainer: View? = null

    @JvmField
    @BindView(R.id.gallery_cta_button_text)
    var ctaButtonText: TextView? = null
    private var unbinder: Unbinder? = null
    private var imageEditType: ImageEditType? = null
    private val disposables = CompositeDisposable()
    private var imageCaptionDisposable: Disposable? = null
    private var revision: Long = 0
    private var sourceWiki: WikiSite? = null
    private var controlsShowing = true
    private var pageChangeListener: GalleryPageChangeListener? = GalleryPageChangeListener()
    private var funnel: GalleryFunnel? = null

    /**
     * If we have an intent that tells us a specific image to jump to within the gallery,
     * then this will be non-null.
     */
    private var initialFilename: String? = null

    /**
     * If we come back from savedInstanceState, then this will be the previous pager position.
     */
    private var initialImageIndex = -1
    private var galleryAdapter: GalleryItemAdapter? = null
    private val downloadReceiver = MediaDownloadReceiver()
    private val downloadReceiverCallback: MediaDownloadReceiverCallback =
        MediaDownloadReceiverCallback()
    private var targetLanguageCode: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        unbinder = ButterKnife.bind(this)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = ""
        setNavigationBarColor(Color.BLACK)
        toolbarGradient!!.background = getPowerGradient(R.color.black26, Gravity.TOP)
        infoGradient!!.background = getPowerGradient(R.color.black38, Gravity.BOTTOM)
        descriptionText!!.movementMethod = linkMovementMethod
        creditText!!.movementMethod = linkMovementMethod
        (errorView!!.findViewById<View>(R.id.view_wiki_error_icon) as ImageView)
            .setColorFilter(ContextCompat.getColor(this, R.color.base70))
        (errorView!!.findViewById<View>(R.id.view_wiki_error_text) as TextView)
            .setTextColor(ContextCompat.getColor(this, R.color.base70))
        errorView!!.backClickListener = View.OnClickListener { v: View? -> onBackPressed() }
        errorView!!.retryClickListener = View.OnClickListener { v: View? ->
            errorView!!.visibility = View.GONE
            loadGalleryContent()
        }
        if (intent.hasExtra(EXTRA_PAGETITLE)) {
            pageTitle = intent.getParcelableExtra(EXTRA_PAGETITLE)
        }
        initialFilename = intent.getStringExtra(EXTRA_FILENAME)
        revision = intent.getLongExtra(EXTRA_REVISION, 0)
        sourceWiki = intent.getParcelableExtra(EXTRA_WIKI)
        galleryAdapter = GalleryItemAdapter(this@GalleryActivity)
        galleryPager!!.adapter = galleryAdapter
        galleryPager!!.registerOnPageChangeCallback(pageChangeListener!!)
        galleryPager!!.offscreenPageLimit = 2
        funnel = GalleryFunnel(
            app, intent.getParcelableExtra(EXTRA_WIKI),
            intent.getIntExtra(EXTRA_SOURCE, 0)
        )
        if (savedInstanceState == null) {
            if (initialFilename != null) {
                funnel!!.logGalleryOpen(pageTitle, initialFilename!!)
            }
        } else {
            controlsShowing = savedInstanceState.getBoolean("controlsShowing")
            initialImageIndex = savedInstanceState.getInt("pagerIndex")
            // if we have a savedInstanceState, then the initial index overrides
            // the initial Title from our intent.
            initialFilename = null
            val fm = supportFragmentManager
            if (supportFragmentManager.backStackEntryCount > 0) {
                val ft = supportFragmentManager.beginTransaction()
                for (i in 0 until fm.backStackEntryCount) {
                    val fragment = fm.findFragmentById(fm.getBackStackEntryAt(i).id)
                    if (fragment is GalleryItemFragment) {
                        ft.remove(fragment)
                    }
                }
                ft.commitAllowingStateLoss()
            }
        }
        toolbarContainer!!.post {
            if (isDestroyed) {
                return@post
            }
            setControlsShowing(controlsShowing)
        }
        if (TRANSITION_INFO != null && TRANSITION_INFO!!.width > 0 && TRANSITION_INFO!!.height > 0) {
            val aspect = TRANSITION_INFO!!.height / TRANSITION_INFO!!.width
            val params = if (displayWidthPx < displayHeightPx) FrameLayout.LayoutParams(
                displayWidthPx, (displayWidthPx * aspect).toInt()
            ) else FrameLayout.LayoutParams(
                (displayHeightPx / aspect).toInt(), displayHeightPx
            )
            params.gravity = Gravity.CENTER
            transitionReceiver!!.layoutParams = params
            transitionReceiver!!.visibility = View.VISIBLE
            loadImage(
                transitionReceiver!!,
                TRANSITION_INFO!!.src,
                TRANSITION_INFO!!.centerCrop,
                false,
                false,
                null
            )
            val transitionMillis = 500
            transitionReceiver!!.postDelayed({
                if (isDestroyed) {
                    return@postDelayed
                }
                loadGalleryContent()
            }, transitionMillis.toLong())
        } else {
            TRANSITION_INFO = null
            transitionReceiver!!.visibility = View.GONE
            loadGalleryContent()
        }
    }

    public override fun onDestroy() {
        disposables.clear()
        disposeImageCaptionDisposable()
        galleryPager!!.unregisterOnPageChangeCallback(pageChangeListener!!)
        pageChangeListener = null
        if (unbinder != null) {
            unbinder!!.unbind()
        }
        TRANSITION_INFO = null
        super.onDestroy()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadReceiver.setCallback(downloadReceiverCallback)
    }

    public override fun onPause() {
        super.onPause()
        downloadReceiver.setCallback(null)
        unregisterReceiver(downloadReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        return true
    }

    override fun onDownload(item: GalleryItemFragment) {
        if (funnel != null && item.imageTitle != null) {
            funnel!!.logGallerySave(pageTitle, item.imageTitle!!.displayText)
        }
        if (item.imageTitle != null && item.mediaInfo != null) {
            downloadReceiver.download(this, item.imageTitle!!, item.mediaInfo!!)
            showMessage(this, R.string.gallery_save_progress)
        } else {
            showMessage(this, R.string.err_cannot_save_file)
        }
    }

    override fun onShare(
        item: GalleryItemFragment,
        bitmap: Bitmap?,
        subject: String,
        title: PageTitle
    ) {
        if (funnel != null && item.imageTitle != null) {
            funnel!!.logGalleryShare(pageTitle, item.imageTitle!!.displayText)
        }
        if (bitmap != null && item.mediaInfo != null) {
            shareImage(
                this, bitmap, File(
                    getUrlForPreferredSize(
                        item.mediaInfo!!.thumbUrl,
                        Constants.PREFERRED_GALLERY_IMAGE_SIZE
                    )
                ).name, subject, title.uri
            )
        } else {
            shareText(this, title)
        }
    }

    override fun setTheme() {
        setTheme(Theme.DARK.resourceId)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT || requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) && resultCode == RESULT_OK) {
            val action =
                if (data != null && data.hasExtra(Constants.INTENT_EXTRA_ACTION)) data.getSerializableExtra(
                    Constants.INTENT_EXTRA_ACTION
                ) as DescriptionEditActivity.Action? else if (requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) DescriptionEditActivity.Action.ADD_IMAGE_TAGS else null
            show(
                this,
                action,
                true,
                targetLanguageCode,
                action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS,
                OpenPageListener {
                    if (action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS && currentItem != null && currentItem!!.imageTitle != null) {
                        startActivity(newIntent(this@GalleryActivity, currentItem!!.imageTitle!!))
                    }
                })
            layOutGalleryDescription()
            setResult(if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT) ACTIVITY_RESULT_IMAGE_CAPTION_ADDED else ACTIVITY_REQUEST_ADD_IMAGE_TAGS)
        }
    }

    @OnClick(R.id.gallery_caption_edit_button)
    fun onEditClick(v: View) {
        val item = currentItem
        if (item == null || item.imageTitle == null || item.mediaInfo == null || item.mediaInfo!!.metadata == null) {
            return
        }
        val isProtected = v.tag != null && v.tag as Boolean
        if (isProtected) {
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.page_protected_can_not_edit_title)
                .setMessage(R.string.page_protected_can_not_edit)
                .setPositiveButton(R.string.protected_page_warning_dialog_ok_button_text, null)
                .show()
            return
        }
        startCaptionEdit(item)
    }

    private fun startCaptionEdit(item: GalleryItemFragment) {
        val title = PageTitle(
            item.imageTitle!!.prefixedText,
            WikiSite(Service.COMMONS_URL, sourceWiki!!.languageCode())
        )
        val currentCaption = item.mediaInfo!!.captions[sourceWiki!!.languageCode()]
        title.description = currentCaption
        val summary = PageSummaryForEdit(
            title.prefixedText, sourceWiki!!.languageCode(), title,
            title.displayText, StringUtils.defaultIfBlank(
                fromHtml(
                    item.mediaInfo!!.metadata!!.imageDescription()
                ).toString(), null
            ),
            item.mediaInfo!!.thumbUrl
        )
        startActivityForResult(
            newIntent(
                this,
                title,
                null,
                summary,
                null,
                DescriptionEditActivity.Action.ADD_CAPTION,
                InvokeSource.GALLERY_ACTIVITY
            ),
            ACTIVITY_REQUEST_DESCRIPTION_EDIT
        )
    }

    @OnClick(R.id.gallery_cta_button)
    fun onTranslateClick(v: View?) {
        val item = currentItem
        if (item == null || item.imageTitle == null || item.mediaInfo == null || item.mediaInfo!!.metadata == null || imageEditType == null) {
            return
        }
        when (imageEditType) {
            ImageEditType.ADD_TAGS -> startTagsEdit(item)
            ImageEditType.ADD_CAPTION_TRANSLATION -> startCaptionTranslation(item)
            else -> startCaptionEdit(item)
        }
    }

    private fun startTagsEdit(item: GalleryItemFragment) {
        startActivityForResult(
            newIntent(this, item.mediaPage!!, InvokeSource.GALLERY_ACTIVITY),
            ACTIVITY_REQUEST_ADD_IMAGE_TAGS
        )
    }

    private fun startCaptionTranslation(item: GalleryItemFragment) {
        val sourceTitle = PageTitle(
            item.imageTitle!!.prefixedText,
            WikiSite(Service.COMMONS_URL, sourceWiki!!.languageCode())
        )
        val targetTitle = PageTitle(
            item.imageTitle!!.prefixedText,
            WikiSite(
                Service.COMMONS_URL,
                StringUtils.defaultString(targetLanguageCode, app.language().appLanguageCodes[1])
            )
        )
        var currentCaption = item.mediaInfo!!.captions[sourceWiki!!.languageCode()]
        if (TextUtils.isEmpty(currentCaption)) {
            currentCaption = fromHtml(
                item.mediaInfo!!.metadata!!.imageDescription()
            ).toString()
        }
        val sourceSummary = PageSummaryForEdit(
            sourceTitle.prefixedText, sourceTitle.wikiSite.languageCode(), sourceTitle,
            sourceTitle.displayText, currentCaption, item.mediaInfo!!.thumbUrl
        )
        val targetSummary = PageSummaryForEdit(
            targetTitle.prefixedText, targetTitle.wikiSite.languageCode(), targetTitle,
            targetTitle.displayText, null, item.mediaInfo!!.thumbUrl
        )
        startActivityForResult(
            newIntent(
                this,
                targetTitle,
                null,
                sourceSummary,
                targetSummary,
                if (sourceSummary.lang == targetSummary.lang) DescriptionEditActivity.Action.ADD_CAPTION else DescriptionEditActivity.Action.TRANSLATE_CAPTION,
                InvokeSource.GALLERY_ACTIVITY
            ),
            ACTIVITY_REQUEST_DESCRIPTION_EDIT
        )
    }

    @OnClick(R.id.gallery_license_container)
    fun onClick(v: View?) {
        if (licenseIcon!!.contentDescription == null) {
            return
        }
        showMessageAsPlainText(
            (licenseIcon!!.context as Activity),
            licenseIcon!!.contentDescription
        )
    }

    @OnLongClick(R.id.gallery_license_container)
    fun onLongClick(v: View?): Boolean {
        val licenseUrl = licenseIcon!!.tag as String
        if (!TextUtils.isEmpty(licenseUrl)) {
            handleExternalLink(
                this@GalleryActivity,
                Uri.parse(resolveProtocolRelativeUrl(licenseUrl))
            )
        }
        return true
    }

    private fun disposeImageCaptionDisposable() {
        if (imageCaptionDisposable != null && !imageCaptionDisposable!!.isDisposed) {
            imageCaptionDisposable!!.dispose()
        }
    }

    private inner class GalleryPageChangeListener : OnPageChangeCallback() {
        private var currentPosition = -1
        override fun onPageSelected(position: Int) {
            // the pager has settled on a new position
            layOutGalleryDescription()
            val item = currentItem
            if (currentPosition != -1 && item != null && item.imageTitle != null && funnel != null) {
                if (position < currentPosition) {
                    funnel!!.logGallerySwipeLeft(pageTitle, item.imageTitle!!.displayText)
                } else if (position > currentPosition) {
                    funnel!!.logGallerySwipeRight(pageTitle, item.imageTitle!!.displayText)
                }
            }
            currentPosition = position
        }

        override fun onPageScrollStateChanged(state: Int) {
            hideTransitionReceiver(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("controlsShowing", controlsShowing)
        outState.putInt("pagerIndex", galleryPager!!.currentItem)
    }

    private fun updateProgressBar(visible: Boolean) {
        progressBar!!.isIndeterminate = true
        progressBar!!.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        // log the "gallery close" event only upon explicit closing of the activity
        // (back button, or home-as-up button in the toolbar)
        val item = currentItem
        if (item != null && item.imageTitle != null && funnel != null) {
            funnel!!.logGalleryClose(pageTitle, item.imageTitle!!.displayText)
        }
        if (TRANSITION_INFO != null) {
            showTransitionReceiver()
        }
        super.onBackPressed()
    }

    fun onMediaLoaded() {
        hideTransitionReceiver(true)
    }

    private fun showTransitionReceiver() {
        transitionReceiver!!.visibility = View.VISIBLE
    }

    private fun hideTransitionReceiver(delay: Boolean) {
        if (transitionReceiver!!.visibility == View.GONE) {
            return
        }
        if (delay) {
            val hideDelayMillis = 250
            transitionReceiver!!.postDelayed({
                if (isDestroyed || transitionReceiver == null) {
                    return@postDelayed
                }
                transitionReceiver!!.visibility = View.GONE
            }, hideDelayMillis.toLong())
        } else {
            transitionReceiver!!.visibility = View.GONE
        }
    }

    /**
     * Show or hide all the UI controls in this activity (slide them out or in).
     * @param showing Whether to show or hide the controls.
     */
    private fun setControlsShowing(showing: Boolean) {
        controlsShowing = showing
        if (controlsShowing) {
            ensureTranslationY(toolbarContainer!!, 0)
            ensureTranslationY(infoContainer!!, 0)
        } else {
            ensureTranslationY(toolbarContainer!!, -toolbarContainer!!.height)
            ensureTranslationY(infoContainer!!, infoContainer!!.height)
        }
    }

    /**
     * Toggle showing or hiding of all the UI controls.
     */
    fun toggleControls() {
        setControlsShowing(!controlsShowing)
    }

    fun showLinkPreview(title: PageTitle) {
        bottomSheetPresenter.show(
            supportFragmentManager,
            newInstance(HistoryEntry(title, HistoryEntry.SOURCE_GALLERY), null)
        )
    }

    fun setViewPagerEnabled(enabled: Boolean) {
        galleryPager!!.isUserInputEnabled = enabled
    }

    /**
     * LinkMovementMethod for handling clicking of links in the description or metadata
     * text fields. For internal links, this activity will close, and pass the page title as
     * the result. For external links, they will be bounced out to the Browser.
     */
    private val linkMovementMethod = LinkMovementMethodExt { urlStr: String ->
        v("Link clicked was $urlStr")
        var url = resolveProtocolRelativeUrl(urlStr)
        if (url.startsWith("/wiki/")) {
            val title = app.wikiSite.titleForInternalLink(url)
            showLinkPreview(title)
        } else {
            val uri = Uri.parse(url)
            val authority = uri.authority
            if (authority != null && WikiSite.supportedAuthority(authority) &&
                uri.path != null && uri.path!!.startsWith("/wiki/")
            ) {
                val title = WikiSite(uri).titleForUri(uri)
                showLinkPreview(title)
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (url.startsWith("/w/")) {
                    url = String.format(
                        "%1\$s://%2\$s", app.wikiSite.scheme(),
                        app.wikiSite.authority()
                    ) + url
                }
                handleExternalLink(this@GalleryActivity, Uri.parse(url))
            }
        }
    }

    /**
     * Close this activity, with the specified PageTitle as the activity result, to be picked up
     * by the activity that originally launched us.
     * @param resultTitle PageTitle to pass as the activity result.
     */
    @JvmOverloads
    fun finishWithPageResult(
        resultTitle: PageTitle,
        historyEntry: HistoryEntry = HistoryEntry(
            resultTitle,
            HistoryEntry.SOURCE_GALLERY
        )
    ) {
        val intent = PageActivity.newIntentForCurrentTab(
            this@GalleryActivity,
            historyEntry,
            resultTitle,
            false
        )
        setResult(ACTIVITY_RESULT_PAGE_SELECTED, intent)
        finish()
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        finishWithPageResult(title, entry)
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        setPlainText(this, null, title.uri)
        showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.showAddToListDialog(
            supportFragmentManager,
            title,
            InvokeSource.LINK_PREVIEW_MENU
        )
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        shareText(this, title)
    }

    fun showError(caught: Throwable?) {
        errorView!!.setError(caught)
        errorView!!.visibility = View.VISIBLE
    }

    private fun fetchGalleryItems() {
        if (pageTitle == null) {
            return
        }
        updateProgressBar(true)
        disposables.add(ServiceFactory.getRest(pageTitle!!.wikiSite)
            .getMediaList(pageTitle!!.prefixedText, revision)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ mediaList: MediaList ->
                applyGalleryList(
                    mediaList.getItems(
                        "image",
                        "video"
                    )
                )
            }) { caught: Throwable? ->
                updateProgressBar(false)
                showError(caught)
            })
    }

    /**
     * Kicks off the activity after the views are initialized in onCreate.
     */
    private fun loadGalleryContent() {
        updateProgressBar(false)
        fetchGalleryItems()
    }

    private fun applyGalleryList(list: List<MediaListItem>) {
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        var list = list
        var initialImagePos = -1
        if (initialFilename != null) {
            for (item in list) {
                // the namespace of a file could be in a different language than English.
                if (removeNamespace(item.title) == removeNamespace(
                        initialFilename!!
                    )
                ) {
                    initialImagePos = list.indexOf(item)
                    break
                }
            }
            if (initialImagePos == -1) {
                // the requested image is not present in the gallery collection, so add it manually.
                // (this can happen if the user clicked on an SVG file, since we hide SVGs
                // by default in the gallery; or lead image in the PageHeader or in the info box)
                initialImagePos = 0
                list = ArrayList(list)
                list.add(initialImagePos, MediaListItem(initialFilename!!))
            }
        }

        // pass the collection to the adapter!
        galleryAdapter!!.setList(list)
        if (initialImagePos != -1) {
            // if we have a target image to jump to, then do it!
            galleryPager!!.setCurrentItem(initialImagePos, false)
        } else if (initialImageIndex >= 0 && initialImageIndex < galleryAdapter!!.itemCount) {
            // if we have a target image index to jump to, then do it!
            galleryPager!!.setCurrentItem(initialImageIndex, false)
        }
    }

    private val currentItem: GalleryItemFragment?
        private get() = galleryAdapter!!.getFragmentAt(galleryPager!!.currentItem) as GalleryItemFragment?

    /**
     * Populate the description and license text fields with data from the current gallery item.
     */
    fun layOutGalleryDescription() {
        val item = currentItem
        if (item?.imageTitle == null || item.mediaInfo == null || item.mediaInfo!!.metadata == null) {
            infoContainer!!.visibility = View.GONE
            return
        }
        updateProgressBar(true)
        disposeImageCaptionDisposable()
        imageCaptionDisposable =
            Observable.zip<Map<String, String>, MwQueryResponse, Map<String, List<String>>, Pair<Boolean, Int>>(
                getImageCaptions(
                    item.imageTitle!!.prefixedText
                ),
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getProtectionInfo(
                    item.imageTitle!!.prefixedText
                ),
                getImageTagsObservable(
                    currentItem!!.mediaPage!!.pageId(),
                    sourceWiki!!.languageCode()
                ),
                Function3 { captions: Map<String?, String?>?, protectionInfoRsp: MwQueryResponse, imageTags: Map<String?, List<String?>?> ->
                    item.mediaInfo!!.captions = captions!!
                    Pair(protectionInfoRsp.query()!!.isEditProtected, imageTags.size)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Consumer { pair: Pair<Boolean, Int> ->
                    updateGalleryDescription(
                        pair.first,
                        pair.second
                    )
                }, Consumer { obj: Throwable -> obj.e() })
    }

    fun updateGalleryDescription(isProtected: Boolean, tagsCount: Int) {
        updateProgressBar(false)
        val item = currentItem
        if (item?.imageTitle == null || item.mediaInfo == null || item.mediaInfo!!.metadata == null) {
            infoContainer!!.visibility = View.GONE
            return
        }
        displayApplicableDescription(item)

        // Display the Caption Edit button based on whether the image is hosted on Commons,
        // and not the local Wikipedia.
        var captionEditable =
            isLoggedIn && item.mediaInfo!!.thumbUrl.contains(Service.URL_FRAGMENT_FROM_COMMONS)
        captionEditButton!!.visibility =
            if (captionEditable) View.VISIBLE else View.GONE
        captionEditButton!!.setImageResource(R.drawable.ic_mode_edit_white_24dp)
        captionEditButton!!.tag = isProtected
        if (isProtected) {
            captionEditButton!!.setImageResource(R.drawable.ic_edit_pencil_locked)
            captionEditable = false
        }
        if (captionEditable) {
            ctaContainer!!.visibility = View.VISIBLE
            decideImageEditType(item, tagsCount)
        } else {
            ctaContainer!!.visibility = View.GONE
        }
        setLicenseInfo(item)
    }

    private fun decideImageEditType(item: GalleryItemFragment, tagsCount: Int) {
        imageEditType = null
        if (!item.mediaInfo!!.captions.containsKey(sourceWiki!!.languageCode())) {
            imageEditType = ImageEditType.ADD_CAPTION
            targetLanguageCode = sourceWiki!!.languageCode()
            ctaButtonText!!.text = getString(R.string.gallery_add_image_caption_button)
            return
        }
        if (tagsCount == 0) {
            imageEditType = ImageEditType.ADD_TAGS
            ctaButtonText!!.text = getString(R.string.suggested_edits_feed_card_add_image_tags)
            return
        }

        // and if we have another language in which the caption doesn't exist, then offer
        // it to be translatable.
        if (app.language().appLanguageCodes.size > 1) {
            for (lang in app.language().appLanguageCodes) {
                if (!item.mediaInfo!!.captions.containsKey(lang)) {
                    targetLanguageCode = lang
                    imageEditType = ImageEditType.ADD_CAPTION_TRANSLATION
                    ctaButtonText!!.text = getString(
                        R.string.gallery_add_image_caption_in_language_button,
                        app.language().getAppLanguageLocalizedName(targetLanguageCode)
                    )
                    break
                }
            }
        }
        ctaContainer!!.visibility = if (imageEditType == null) View.GONE else View.VISIBLE
    }

    private fun displayApplicableDescription(item: GalleryItemFragment) {
        // If we have a structured caption in our current language, then display that instead
        // of the unstructured description, and make it editable.
        val descriptionStr: CharSequence?
        descriptionStr = if (item.mediaInfo!!.captions.containsKey(sourceWiki!!.languageCode())) {
            item.mediaInfo!!.captions[sourceWiki!!.languageCode()]
        } else {
            fromHtml(
                item.mediaInfo!!.metadata!!.imageDescription()
            )
        }
        if (descriptionStr != null && descriptionStr.length > 0) {
            galleryDescriptionContainer!!.visibility = View.VISIBLE
            descriptionText!!.text = strip(descriptionStr)
        } else {
            galleryDescriptionContainer!!.visibility = View.GONE
        }
    }

    private fun setLicenseInfo(item: GalleryItemFragment) {
        val license = ImageLicense(
            item.mediaInfo!!.metadata!!.license(),
            item.mediaInfo!!.metadata!!.licenseShortName(),
            item.mediaInfo!!.metadata!!
                .licenseUrl()
        )

        // determine which icon to display...
        if (license.licenseIcon == R.drawable.ic_license_by) {
            licenseIcon!!.setImageResource(R.drawable.ic_license_cc)
            byIcon!!.setImageResource(R.drawable.ic_license_by)
            byIcon!!.visibility = View.VISIBLE
            saIcon!!.setImageResource(R.drawable.ic_license_sharealike)
            saIcon!!.visibility = View.VISIBLE
        } else {
            licenseIcon!!.setImageResource(license.licenseIcon)
            byIcon!!.visibility = View.GONE
            saIcon!!.visibility = View.GONE
        }

        // Set the icon's content description to the UsageTerms property.
        // (if UsageTerms is not present, then default to Fair Use)
        licenseIcon!!.contentDescription = StringUtils.defaultIfBlank(
            item.mediaInfo!!.metadata!!.licenseShortName(),
            getString(R.string.gallery_fair_use_license)
        )
        // Give the license URL to the icon, to be received by the click handler (may be null).
        licenseIcon!!.tag = item.mediaInfo!!.metadata!!.licenseUrl()
        setContextClickAsLongClick(licenseContainer!!)
        val creditStr = if (!TextUtils.isEmpty(
                item.mediaInfo!!.metadata!!.artist()
            )
        ) item.mediaInfo!!.metadata!!
            .artist() else item.mediaInfo!!.metadata!!.credit()

        // if we couldn't find a attribution string, then default to unknown
        creditText!!.text = fromHtml(
            StringUtils.defaultIfBlank(
                creditStr,
                getString(R.string.gallery_uploader_unknown)
            )
        )
        infoContainer!!.visibility = View.VISIBLE
    }

    /**
     * Adapter that will provide the contents for the ViewPager.
     * Each media item will be represented by a GalleryItemFragment, which will be instantiated
     * lazily, and then cached for future use.
     */
    private inner class GalleryItemAdapter internal constructor(activity: AppCompatActivity?) :
        PositionAwareFragmentStateAdapter(
            activity!!
        ) {
        private val list: MutableList<MediaListItem> = ArrayList()
        fun setList(list: List<MediaListItem>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun createFragment(position: Int): Fragment {
            return newInstance(pageTitle, list[position])
        }
    }

    private inner class MediaDownloadReceiverCallback : MediaDownloadReceiver.Callback {
        override fun onSuccess() {
            showMessage(this@GalleryActivity, R.string.gallery_save_success)
        }
    }

    companion object {
        const val ACTIVITY_RESULT_PAGE_SELECTED = 1
        private const val ACTIVITY_REQUEST_DESCRIPTION_EDIT = 2
        const val ACTIVITY_RESULT_IMAGE_CAPTION_ADDED = 3
        const val ACTIVITY_REQUEST_ADD_IMAGE_TAGS = 4
        const val EXTRA_PAGETITLE = "pageTitle"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_WIKI = "wiki"
        const val EXTRA_REVISION = "revision"
        const val EXTRA_SOURCE = "source"
        private var TRANSITION_INFO: ImageHitInfo? = null

        @JvmStatic
        fun newIntent(
            context: Context, pageTitle: PageTitle?,
            filename: String, wiki: WikiSite, revision: Long, source: Int
        ): Intent {
            val intent = Intent()
                .setClass(context, GalleryActivity::class.java)
                .putExtra(EXTRA_FILENAME, filename)
                .putExtra(EXTRA_WIKI, wiki)
                .putExtra(EXTRA_REVISION, revision)
                .putExtra(EXTRA_SOURCE, source)
            if (pageTitle != null) {
                intent.putExtra(EXTRA_PAGETITLE, pageTitle)
            }
            return intent
        }

        @JvmStatic
        fun setTransitionInfo(hitInfo: ImageHitInfo) {
            TRANSITION_INFO = hitInfo
        }
    }
}
