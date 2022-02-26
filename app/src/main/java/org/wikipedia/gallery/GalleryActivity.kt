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
import android.util.Pair
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.ImageEditType
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.commons.ImageTagsProvider
import org.wikipedia.databinding.ActivityGalleryBinding
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.media.MediaHelper
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.theme.Theme
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.PositionAwareFragmentStateAdapter
import org.wikipedia.views.ViewAnimations
import org.wikipedia.views.ViewUtil
import java.io.File

class GalleryActivity : BaseActivity(), LinkPreviewDialog.Callback, GalleryItemFragment.Callback {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var sourceWiki: WikiSite
    private lateinit var funnel: GalleryFunnel
    private lateinit var galleryAdapter: GalleryItemAdapter
    private var pageChangeListener = GalleryPageChangeListener()
    private var pageTitle: PageTitle? = null
    private var imageEditType: ImageEditType? = null
    private val disposables = CompositeDisposable()
    private var imageCaptionDisposable: Disposable? = null
    private var revision = 0L
    private var controlsShowing = true
    /**
     * If we have an intent that tells us a specific image to jump to within the gallery,
     * then this will be non-null.
     */
    private var initialFilename: String? = null
    /**
     * If we come back from savedInstanceState, then this will be the previous pager position.
     */
    private var initialImageIndex = -1
    private var targetLanguageCode: String? = null
    private val app = WikipediaApp.getInstance()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val downloadReceiver = MediaDownloadReceiver()
    private val downloadReceiverCallback = MediaDownloadReceiverCallback()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = ""
        setNavigationBarColor(Color.BLACK)
        binding.toolbarGradient.background = GradientUtil.getPowerGradient(R.color.black26, Gravity.TOP)
        binding.infoGradient.background = GradientUtil.getPowerGradient(R.color.black38, Gravity.BOTTOM)
        binding.descriptionText.movementMethod = linkMovementMethod
        binding.creditText.movementMethod = linkMovementMethod
        binding.errorView.setIconColorFilter(ContextCompat.getColor(this, R.color.base70))
        binding.errorView.setErrorTextColor(ContextCompat.getColor(this, R.color.base70))
        binding.errorView.backClickListener = View.OnClickListener { onBackPressed() }
        binding.errorView.retryClickListener = View.OnClickListener {
            binding.errorView.visibility = View.GONE
            loadGalleryContent()
        }
        if (intent.hasExtra(EXTRA_PAGETITLE)) {
            pageTitle = intent.getParcelableExtra(EXTRA_PAGETITLE)
        }
        initialFilename = intent.getStringExtra(EXTRA_FILENAME)
        revision = intent.getLongExtra(EXTRA_REVISION, 0)
        sourceWiki = intent.getParcelableExtra(EXTRA_WIKI)!!
        galleryAdapter = GalleryItemAdapter(this@GalleryActivity)
        binding.pager.adapter = galleryAdapter
        binding.pager.registerOnPageChangeCallback(pageChangeListener)
        binding.pager.offscreenPageLimit = 2
        funnel = GalleryFunnel(app, intent.getParcelableExtra(EXTRA_WIKI), intent.getIntExtra(EXTRA_SOURCE, 0))
        if (savedInstanceState == null) {
            initialFilename?.let {
                funnel.logGalleryOpen(pageTitle, it)
            }
        } else {
            controlsShowing = savedInstanceState.getBoolean(KEY_CONTROLS_SHOWING)
            initialImageIndex = savedInstanceState.getInt(KEY_PAGER_INDEX)
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
        binding.toolbarContainer.post {
            if (isDestroyed) {
                return@post
            }
            setControlsShowing(controlsShowing)
        }
        if (TRANSITION_INFO != null && TRANSITION_INFO!!.width > 0 && TRANSITION_INFO!!.height > 0) {
            val aspect = TRANSITION_INFO!!.height / TRANSITION_INFO!!.width
            val params = if (DimenUtil.displayWidthPx < DimenUtil.displayHeightPx) FrameLayout.LayoutParams(DimenUtil.displayWidthPx, (DimenUtil.displayWidthPx * aspect).toInt())
            else FrameLayout.LayoutParams((DimenUtil.displayHeightPx / aspect).toInt(), DimenUtil.displayHeightPx)
            params.gravity = Gravity.CENTER
            binding.transitionReceiver.layoutParams = params
            binding.transitionReceiver.visibility = View.VISIBLE
            ViewUtil.loadImage(binding.transitionReceiver, TRANSITION_INFO!!.src, TRANSITION_INFO!!.centerCrop,
                largeRoundedSize = false, force = false, listener = null)
            val transitionMillis = 500
            binding.transitionReceiver.postDelayed({
                if (isDestroyed) {
                    return@postDelayed
                }
                loadGalleryContent()
            }, transitionMillis.toLong())
        } else {
            TRANSITION_INFO = null
            binding.transitionReceiver.visibility = View.GONE
            loadGalleryContent()
        }
        binding.captionEditButton.setOnClickListener { onEditClick(it) }
        binding.ctaButton.setOnClickListener { onTranslateClick() }
        binding.licenseContainer.setOnClickListener { onLicenseClick() }
        binding.licenseContainer.setOnLongClickListener { onLicenseLongClick() }
    }

    public override fun onDestroy() {
        disposables.clear()
        disposeImageCaptionDisposable()
        binding.pager.unregisterOnPageChangeCallback(pageChangeListener)
        TRANSITION_INFO = null
        super.onDestroy()
    }

    public override fun onResume() {
        super.onResume()
        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadReceiver.callback = downloadReceiverCallback
    }

    public override fun onPause() {
        super.onPause()
        downloadReceiver.callback = null
        unregisterReceiver(downloadReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        return true
    }

    override fun onDownload(item: GalleryItemFragment) {
        item.imageTitle?.let {
            funnel.logGallerySave(pageTitle, it.displayText)
        }
        if (item.imageTitle != null && item.mediaInfo != null) {
            downloadReceiver.download(this, item.imageTitle!!, item.mediaInfo!!)
            FeedbackUtil.showMessage(this, R.string.gallery_save_progress)
        } else {
            FeedbackUtil.showMessage(this, R.string.err_cannot_save_file)
        }
    }

    override fun onShare(item: GalleryItemFragment, bitmap: Bitmap?, subject: String, title: PageTitle) {
        item.imageTitle?.let {
            funnel.logGalleryShare(pageTitle, it.displayText)
        }
        if (bitmap != null && item.mediaInfo != null) {
            ShareUtil.shareImage(this, bitmap,
                File(ImageUrlUtil.getUrlForPreferredSize(item.mediaInfo!!.thumbUrl, Constants.PREFERRED_GALLERY_IMAGE_SIZE)).name,
                subject, title.uri)
        } else {
            ShareUtil.shareText(this, title)
        }
    }

    override fun setTheme() {
        setTheme(Theme.DARK.resourceId)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT || requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) && resultCode == RESULT_OK) {
            val action = if (data != null && data.hasExtra(Constants.INTENT_EXTRA_ACTION)) data.getSerializableExtra(
                        Constants.INTENT_EXTRA_ACTION) as DescriptionEditActivity.Action?
                        else if (requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) DescriptionEditActivity.Action.ADD_IMAGE_TAGS else null
            SuggestedEditsSnackbars.show(this, action, true,
                targetLanguageCode, action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS) {
                currentItem?.let {
                    if (action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS && it.imageTitle != null) {
                        startActivity(FilePageActivity.newIntent(this@GalleryActivity, it.imageTitle!!))
                    }
                }
            }
            layOutGalleryDescription(currentItem)
            setResult(if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT) ACTIVITY_RESULT_IMAGE_CAPTION_ADDED else ACTIVITY_REQUEST_ADD_IMAGE_TAGS)
        }
    }

    private fun onEditClick(v: View) {
        val item = currentItem
        if (item?.imageTitle == null || item.mediaInfo?.metadata == null) {
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
        val title = PageTitle(item.imageTitle!!.prefixedText,
            WikiSite(Service.COMMONS_URL, sourceWiki.languageCode))
        val currentCaption = item.mediaInfo!!.captions[sourceWiki.languageCode]
        title.description = currentCaption
        val summary = PageSummaryForEdit(title.prefixedText, sourceWiki.languageCode, title,
            title.displayText, RichTextUtil.stripHtml(item.mediaInfo!!.metadata!!.imageDescription()), item.mediaInfo!!.thumbUrl)
        startActivityForResult(DescriptionEditActivity.newIntent(this, title, null, summary, null,
            DescriptionEditActivity.Action.ADD_CAPTION, InvokeSource.GALLERY_ACTIVITY), ACTIVITY_REQUEST_DESCRIPTION_EDIT)
    }

    private fun onTranslateClick() {
        val item = currentItem
        if (item?.imageTitle == null || item.mediaInfo?.metadata == null || imageEditType == null) {
            return
        }
        when (imageEditType) {
            ImageEditType.ADD_TAGS -> startTagsEdit(item)
            ImageEditType.ADD_CAPTION_TRANSLATION -> startCaptionTranslation(item)
            else -> startCaptionEdit(item)
        }
    }

    private fun startTagsEdit(item: GalleryItemFragment) {
        startActivityForResult(SuggestedEditsImageTagEditActivity.newIntent(this, item.mediaPage!!,
            InvokeSource.GALLERY_ACTIVITY), ACTIVITY_REQUEST_ADD_IMAGE_TAGS)
    }

    private fun startCaptionTranslation(item: GalleryItemFragment) {
        val sourceTitle = PageTitle(item.imageTitle!!.prefixedText, WikiSite(Service.COMMONS_URL, sourceWiki.languageCode))
        val targetTitle = PageTitle(item.imageTitle!!.prefixedText, WikiSite(Service.COMMONS_URL,
            targetLanguageCode ?: app.language().appLanguageCodes[1]))
        val currentCaption = item.mediaInfo!!.captions[sourceWiki.languageCode].orEmpty().ifEmpty {
            RichTextUtil.stripHtml(item.mediaInfo!!.metadata!!.imageDescription())
        }
        val sourceSummary = PageSummaryForEdit(sourceTitle.prefixedText, sourceTitle.wikiSite.languageCode,
                            sourceTitle, sourceTitle.displayText, currentCaption, item.mediaInfo!!.thumbUrl)
        val targetSummary = PageSummaryForEdit(targetTitle.prefixedText, targetTitle.wikiSite.languageCode,
            targetTitle, targetTitle.displayText, null, item.mediaInfo!!.thumbUrl)
        startActivityForResult(DescriptionEditActivity.newIntent(this, targetTitle, null, sourceSummary,
            targetSummary, if (sourceSummary.lang == targetSummary.lang) DescriptionEditActivity.Action.ADD_CAPTION
            else DescriptionEditActivity.Action.TRANSLATE_CAPTION, InvokeSource.GALLERY_ACTIVITY), ACTIVITY_REQUEST_DESCRIPTION_EDIT)
    }

    private fun onLicenseClick() {
        if (binding.licenseIcon.contentDescription == null) {
            return
        }
        FeedbackUtil.showMessageAsPlainText((binding.licenseIcon.context as Activity),
            binding.licenseIcon.contentDescription)
    }

    private fun onLicenseLongClick(): Boolean {
        val licenseUrl = binding.licenseIcon.tag as String
        if (licenseUrl.isNotEmpty()) {
            UriUtil.handleExternalLink(this@GalleryActivity,
                Uri.parse(UriUtil.resolveProtocolRelativeUrl(licenseUrl)))
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
            currentItem?.let { item ->
                layOutGalleryDescription(item)
                item.imageTitle?.let {
                    if (currentPosition != -1) {
                        if (position < currentPosition) {
                            funnel.logGallerySwipeLeft(pageTitle, it.displayText)
                        } else if (position > currentPosition) {
                            funnel.logGallerySwipeRight(pageTitle, it.displayText)
                        }
                    }
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
        outState.putBoolean(KEY_CONTROLS_SHOWING, controlsShowing)
        outState.putInt(KEY_PAGER_INDEX, binding.pager.currentItem)
    }

    private fun updateProgressBar(visible: Boolean) {
        binding.progressBar.isIndeterminate = true
        binding.progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onBackPressed() {
        // log the "gallery close" event only upon explicit closing of the activity
        // (back button, or home-as-up button in the toolbar)
        currentItem?.imageTitle?.let {
            funnel.logGalleryClose(pageTitle, it.displayText)
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
        binding.transitionReceiver.visibility = View.VISIBLE
    }

    private fun hideTransitionReceiver(delay: Boolean) {
        if (binding.transitionReceiver.visibility == View.GONE) {
            return
        }
        if (delay) {
            val hideDelayMillis = 250L
            binding.transitionReceiver.postDelayed({
                if (isDestroyed) {
                    return@postDelayed
                }
                binding.transitionReceiver.visibility = View.GONE
            }, hideDelayMillis)
        } else {
            binding.transitionReceiver.visibility = View.GONE
        }
    }

    private fun setControlsShowing(showing: Boolean) {
        controlsShowing = showing
        if (controlsShowing) {
            ViewAnimations.ensureTranslationY(binding.toolbarContainer, 0)
            ViewAnimations.ensureTranslationY(binding.infoContainer, 0)
        } else {
            ViewAnimations.ensureTranslationY(binding.toolbarContainer, -binding.toolbarContainer.height)
            ViewAnimations.ensureTranslationY(binding.infoContainer, binding.infoContainer.height)
        }
        binding.descriptionText.setTextIsSelectable(controlsShowing)
    }

    fun toggleControls() {
        setControlsShowing(!controlsShowing)
    }

    private fun showLinkPreview(title: PageTitle) {
        bottomSheetPresenter.show(supportFragmentManager,
            LinkPreviewDialog.newInstance(HistoryEntry(title, HistoryEntry.SOURCE_GALLERY), null))
    }

    fun setViewPagerEnabled(enabled: Boolean) {
        binding.pager.isUserInputEnabled = enabled
    }

    private val linkMovementMethod = LinkMovementMethodExt { urlStr ->
        L.v("Link clicked was $urlStr")
        var url = UriUtil.resolveProtocolRelativeUrl(urlStr)
        if (url.startsWith("/wiki/")) {
            val title = app.wikiSite.titleForInternalLink(url)
            showLinkPreview(title)
        } else {
            val uri = Uri.parse(url)
            val authority = uri.authority
            if (authority != null && WikiSite.supportedAuthority(authority) &&
                uri.path != null && uri.path!!.startsWith("/wiki/")) {
                val title = WikiSite(uri).titleForUri(uri)
                showLinkPreview(title)
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (url.startsWith("/w/")) {
                    url = String.format("%1\$s://%2\$s", app.wikiSite.scheme(),
                        app.wikiSite.authority()) + url
                }
                UriUtil.handleExternalLink(this@GalleryActivity, Uri.parse(url))
            }
        }
    }

    private fun finishWithPageResult(resultTitle: PageTitle, historyEntry: HistoryEntry = HistoryEntry(resultTitle, HistoryEntry.SOURCE_GALLERY)) {
        val intent = PageActivity.newIntentForCurrentTab(this@GalleryActivity, historyEntry, resultTitle, false)
        setResult(ACTIVITY_RESULT_PAGE_SELECTED, intent)
        finish()
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        finishWithPageResult(title, entry)
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, null, title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.showAddToListDialog(supportFragmentManager, title, InvokeSource.LINK_PREVIEW_MENU)
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    fun showError(caught: Throwable?) {
        binding.errorView.setError(caught)
        binding.errorView.visibility = View.VISIBLE
    }

    private fun fetchGalleryItems() {
        pageTitle?.let {
            updateProgressBar(true)
            disposables.add(ServiceFactory.getRest(it.wikiSite)
                .getMediaList(it.prefixedText, revision)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ mediaList ->
                    applyGalleryList(mediaList.getItems("image", "video"))
                }) { caught ->
                    updateProgressBar(false)
                    showError(caught)
                })
        }
    }

    private fun loadGalleryContent() {
        updateProgressBar(false)
        fetchGalleryItems()
    }

    private fun applyGalleryList(mediaListItems: MutableList<MediaListItem>) {
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        var initialImagePos = -1
        initialFilename?.let {
            for (item in mediaListItems) {
                // the namespace of a file could be in a different language than English.
                if (StringUtil.removeNamespace(item.title) == StringUtil.removeNamespace(it)) {
                    initialImagePos = mediaListItems.indexOf(item)
                    break
                }
            }
            if (initialImagePos == -1) {
                // the requested image is not present in the gallery collection, so add it manually.
                // (this can happen if the user clicked on an SVG file, since we hide SVGs
                // by default in the gallery; or lead image in the PageHeader or in the info box)
                initialImagePos = 0
                mediaListItems.add(initialImagePos, MediaListItem(it))
            }
        }

        // pass the collection to the adapter!
        galleryAdapter.setList(mediaListItems)
        if (initialImagePos != -1) {
            // if we have a target image to jump to, then do it!
            binding.pager.setCurrentItem(initialImagePos, false)
        } else if (initialImageIndex >= 0 && initialImageIndex < galleryAdapter.itemCount) {
            // if we have a target image index to jump to, then do it!
            binding.pager.setCurrentItem(initialImageIndex, false)
        }
    }

    private val currentItem get() = galleryAdapter.getFragmentAt(binding.pager.currentItem) as GalleryItemFragment?

    fun layOutGalleryDescription(callingFragment: GalleryItemFragment?) {
        val item = currentItem
        if (item != callingFragment) {
            return
        }
        if (item?.imageTitle == null || item.mediaInfo?.metadata == null) {
            binding.infoContainer.visibility = View.GONE
            return
        }
        updateProgressBar(true)
        disposeImageCaptionDisposable()
        imageCaptionDisposable =
            Observable.zip<Map<String, String>, MwQueryResponse, Map<String, List<String>>, Pair<Boolean, Int>>(
                MediaHelper.getImageCaptions(item.imageTitle!!.prefixedText),
                ServiceFactory.get(WikiSite(Service.COMMONS_URL)).getProtectionInfo(item.imageTitle!!.prefixedText),
                ImageTagsProvider.getImageTagsObservable(currentItem!!.mediaPage!!.pageId, sourceWiki.languageCode),
                { captions, protectionInfoRsp, imageTags ->
                    item.mediaInfo!!.captions = captions
                    Pair(protectionInfoRsp.query?.isEditProtected, imageTags.size)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateGalleryDescription(it.first, it.second)
                }, {
                    L.e(it)
                    updateGalleryDescription(false, 0)
                })
    }

    fun updateGalleryDescription(isProtected: Boolean, tagsCount: Int) {
        updateProgressBar(false)
        val item = currentItem
        if (item?.imageTitle == null || item.mediaInfo?.metadata == null) {
            binding.infoContainer.visibility = View.GONE
            return
        }
        displayApplicableDescription(item)

        // Display the Caption Edit button based on whether the image is hosted on Commons,
        // and not the local Wikipedia.
        var captionEditable = AccountUtil.isLoggedIn && item.mediaInfo!!.thumbUrl.contains(Service.URL_FRAGMENT_FROM_COMMONS)
        binding.captionEditButton.visibility = if (captionEditable) View.VISIBLE else View.GONE
        binding.captionEditButton.setImageResource(R.drawable.ic_mode_edit_white_24dp)
        binding.captionEditButton.tag = isProtected
        if (isProtected) {
            binding.captionEditButton.setImageResource(R.drawable.ic_edit_pencil_locked)
            captionEditable = false
        }
        if (captionEditable) {
            binding.ctaContainer.visibility = View.VISIBLE
            decideImageEditType(item, tagsCount)
        } else {
            binding.ctaContainer.visibility = View.GONE
        }
        setLicenseInfo(item)
    }

    private fun decideImageEditType(item: GalleryItemFragment, tagsCount: Int) {
        imageEditType = null
        if (!item.mediaInfo!!.captions.containsKey(sourceWiki.languageCode)) {
            imageEditType = ImageEditType.ADD_CAPTION
            targetLanguageCode = sourceWiki.languageCode
            binding.ctaButtonText.text = getString(R.string.gallery_add_image_caption_button)
            return
        }
        if (tagsCount == 0) {
            imageEditType = ImageEditType.ADD_TAGS
            binding.ctaButtonText.text = getString(R.string.suggested_edits_feed_card_add_image_tags)
            return
        }

        // and if we have another language in which the caption doesn't exist, then offer
        // it to be translatable.
        if (app.language().appLanguageCodes.size > 1) {
            for (lang in app.language().appLanguageCodes) {
                if (!item.mediaInfo!!.captions.containsKey(lang)) {
                    targetLanguageCode = lang
                    imageEditType = ImageEditType.ADD_CAPTION_TRANSLATION
                    binding.ctaButtonText.text = getString(R.string.gallery_add_image_caption_in_language_button,
                        app.language().getAppLanguageLocalizedName(targetLanguageCode))
                    break
                }
            }
        }
        binding.ctaContainer.visibility = if (imageEditType == null) View.GONE else View.VISIBLE
    }

    private fun displayApplicableDescription(item: GalleryItemFragment) {
        // If we have a structured caption in our current language, then display that instead
        // of the unstructured description, and make it editable.
        val descriptionStr = item.mediaInfo?.captions!!.getOrElse(sourceWiki.languageCode) {
            StringUtil.fromHtml(item.mediaInfo!!.metadata!!.imageDescription())
        }

        if (descriptionStr.isNotEmpty()) {
            binding.descriptionContainer.visibility = View.VISIBLE
            binding.descriptionText.text = StringUtil.strip(descriptionStr)
        } else {
            binding.descriptionContainer.visibility = View.GONE
        }
    }

    private fun setLicenseInfo(item: GalleryItemFragment) {
        val metadata = item.mediaInfo!!.metadata!!
        val license = ImageLicense(metadata.license(), metadata.licenseShortName(), metadata.licenseUrl())

        // determine which icon to display...
        if (license.licenseIcon == R.drawable.ic_license_by) {
            binding.licenseIcon.setImageResource(R.drawable.ic_license_cc)
            binding.licenseIconBy.setImageResource(R.drawable.ic_license_by)
            binding.licenseIconBy.visibility = View.VISIBLE
            binding.licenseIconSa.setImageResource(R.drawable.ic_license_sharealike)
            binding.licenseIconSa.visibility = View.VISIBLE
        } else {
            binding.licenseIcon.setImageResource(license.licenseIcon)
            binding.licenseIconBy.visibility = View.GONE
            binding.licenseIconSa.visibility = View.GONE
        }

        // Set the icon's content description to the UsageTerms property.
        // (if UsageTerms is not present, then default to Fair Use)
        binding.licenseIcon.contentDescription = metadata.licenseShortName().ifBlank {
            getString(R.string.gallery_fair_use_license)
        }
        // Give the license URL to the icon, to be received by the click handler (may be null).
        binding.licenseIcon.tag = metadata.licenseUrl()
        DeviceUtil.setContextClickAsLongClick(binding.licenseContainer)
        val creditStr = metadata.artist().ifEmpty { metadata.credit() }

        // if we couldn't find a attribution string, then default to unknown
        binding.creditText.text = StringUtil.fromHtml(creditStr.ifBlank { getString(R.string.gallery_uploader_unknown) })
        binding.infoContainer.visibility = View.VISIBLE
    }

    private inner class GalleryItemAdapter(activity: AppCompatActivity) : PositionAwareFragmentStateAdapter(activity) {
        private val list = mutableListOf<MediaListItem>()
        fun setList(list: List<MediaListItem>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun createFragment(position: Int): Fragment {
            return GalleryItemFragment.newInstance(pageTitle, list[position])
        }
    }

    private inner class MediaDownloadReceiverCallback : MediaDownloadReceiver.Callback {
        override fun onSuccess() {
            FeedbackUtil.showMessage(this@GalleryActivity, R.string.gallery_save_success)
        }
    }

    companion object {
        private const val KEY_CONTROLS_SHOWING = "controlsShowing"
        private const val KEY_PAGER_INDEX = "pagerIndex"
        const val ACTIVITY_RESULT_PAGE_SELECTED = 1
        const val ACTIVITY_REQUEST_DESCRIPTION_EDIT = 2
        const val ACTIVITY_RESULT_IMAGE_CAPTION_ADDED = 3
        const val ACTIVITY_REQUEST_ADD_IMAGE_TAGS = 4
        const val EXTRA_PAGETITLE = "pageTitle"
        const val EXTRA_FILENAME = "filename"
        const val EXTRA_WIKI = "wiki"
        const val EXTRA_REVISION = "revision"
        const val EXTRA_SOURCE = "source"
        private var TRANSITION_INFO: JavaScriptActionHandler.ImageHitInfo? = null

        fun newIntent(context: Context, pageTitle: PageTitle?, filename: String, wiki: WikiSite, revision: Long, source: Int): Intent {
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

        fun setTransitionInfo(hitInfo: JavaScriptActionHandler.ImageHitInfo) {
            TRANSITION_INFO = hitInfo
        }
    }
}
