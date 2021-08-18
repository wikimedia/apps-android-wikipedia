package org.wikipedia.page.linkpreview

import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.analytics.LinkPreviewFunnel
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.DialogLinkPreviewBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.GalleryThumbnailScrollView.GalleryViewListener
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewUtil
import java.util.*

class LinkPreviewDialog : ExtendedBottomSheetDialogFragment(), LinkPreviewErrorView.Callback, DialogInterface.OnDismissListener {
    interface Callback {
        fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean)
        fun onLinkPreviewCopyLink(title: PageTitle)
        fun onLinkPreviewAddToList(title: PageTitle)
        fun onLinkPreviewShareLink(title: PageTitle)
    }

    private var _binding: DialogLinkPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyEntry: HistoryEntry
    private lateinit var pageTitle: PageTitle
    private lateinit var funnel: LinkPreviewFunnel
    private var location: Location? = null
    private var overlayView: LinkPreviewOverlayView? = null
    private var navigateSuccess = false
    private var revision: Long = 0
    private val disposables = CompositeDisposable()

    private val menuListener = PopupMenu.OnMenuItemClickListener { item ->
        return@OnMenuItemClickListener when (item.itemId) {
            R.id.menu_link_preview_add_to_list -> {
                callback()?.onLinkPreviewAddToList(pageTitle)
                true
            }
            R.id.menu_link_preview_share_page -> {
                callback()?.onLinkPreviewShareLink(pageTitle)
                true
            }
            R.id.menu_link_preview_copy_link -> {
                callback()?.onLinkPreviewCopyLink(pageTitle)
                dismiss()
                true
            }
            else -> false
        }
    }
    private val galleryViewListener = GalleryViewListener { view, thumbUrl, imageName ->
        var options: ActivityOptionsCompat? = null
        view.drawable?.let {
            val hitInfo = JavaScriptActionHandler.ImageHitInfo(0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat(), thumbUrl, false)
            GalleryActivity.setTransitionInfo(hitInfo)
            view.transitionName = requireActivity().getString(R.string.transition_page_gallery)
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, requireActivity().getString(R.string.transition_page_gallery))
        }
        startActivityForResult(GalleryActivity.newIntent(requireContext(), pageTitle, imageName,
                pageTitle.wikiSite, revision, GalleryFunnel.SOURCE_LINK_PREVIEW),
                Constants.ACTIVITY_REQUEST_GALLERY, options?.toBundle())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLinkPreviewBinding.inflate(inflater, container, false)

        historyEntry = requireArguments().getParcelable(ARG_ENTRY)!!
        pageTitle = historyEntry.title
        location = requireArguments().getParcelable(ARG_LOCATION)

        binding.linkPreviewToolbar.setOnClickListener { goToLinkedPage(false) }
        binding.linkPreviewOverflowButton.setOnClickListener {
            PopupMenu(requireActivity(), binding.linkPreviewOverflowButton).run {
                inflate(R.menu.menu_link_preview)
                setOnMenuItemClickListener(menuListener)
                show()
            }
        }
        L10nUtil.setConditionalLayoutDirection(binding.root, pageTitle.wikiSite.languageCode)
        loadContent()
        funnel = LinkPreviewFunnel(WikipediaApp.getInstance(), historyEntry.source)
        funnel.logLinkClick()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val containerView = requireDialog().findViewById<ViewGroup>(R.id.container)
        if (overlayView == null && containerView != null) {
            LinkPreviewOverlayView(requireContext()).let {
                overlayView = it
                it.callback = OverlayViewCallback()
                it.setPrimaryButtonText(L10nUtil.getStringForArticleLanguage(pageTitle,
                        if (pageTitle.namespace() === Namespace.TALK || pageTitle.namespace() === Namespace.USER_TALK) R.string.button_continue_to_talk_page else R.string.button_continue_to_article))
                it.setSecondaryButtonText(L10nUtil.getStringForArticleLanguage(pageTitle, R.string.menu_long_press_open_in_new_tab))
                it.showTertiaryButton(location != null)
                containerView.addView(it)
            }
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.linkPreviewThumbnailGallery.listener = null
        binding.linkPreviewToolbar.setOnClickListener(null)
        binding.linkPreviewOverflowButton.setOnClickListener(null)
        overlayView?.let {
            it.callback = null
            overlayView = null
        }
        _binding = null
        super.onDestroyView()
    }

    override fun onDismiss(dialogInterface: DialogInterface) {
        super.onDismiss(dialogInterface)
        if (!navigateSuccess) {
            funnel.logCancel()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED) {
            startActivity(data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onAddToList() {
        callback()?.onLinkPreviewAddToList(pageTitle)
    }

    override fun onDismiss() {
        dismiss()
    }

    private fun loadContent() {
        binding.linkPreviewProgress.visibility = View.VISIBLE
        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getSummaryResponse(pageTitle.prefixedText, null, null, null, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    val summary = response.body()!!
                    funnel.setPageId(summary.pageId)
                    revision = summary.revision

                    // Rebuild our PageTitle, since it may have been redirected or normalized.
                    val oldFragment = pageTitle.fragment
                    pageTitle = PageTitle(summary.apiTitle, pageTitle.wikiSite, summary.thumbnailUrl,
                            summary.description, summary.displayTitle)

                    // check if our URL was redirected, which might include a URL fragment that leads
                    // to a specific section in the target article.
                    if (!response.raw().request.url.fragment.isNullOrEmpty()) {
                        pageTitle.fragment = response.raw().request.url.fragment
                    } else if (!oldFragment.isNullOrEmpty()) {
                        pageTitle.fragment = oldFragment
                    }
                    binding.linkPreviewTitle.text = StringUtil.fromHtml(summary.displayTitle)
                    showPreview(LinkPreviewContents(summary, pageTitle.wikiSite))
                }) { caught ->
                    L.e(caught)
                    binding.linkPreviewTitle.text = StringUtil.fromHtml(pageTitle.displayText)
                    showError(caught)
                })
    }

    private fun loadGallery() {
        if (Prefs.isImageDownloadEnabled()) {
            disposables.add(ServiceFactory.getRest(pageTitle.wikiSite).getMediaList(pageTitle.prefixedText, revision)
                    .flatMap { mediaList ->
                        val maxImages = 10
                        val items = mediaList.getItems("image", "video").asReversed()
                        val titleList = items.filter { it.showInGallery }.map { it.title }.take(maxImages)
                        if (titleList.isEmpty()) Observable.empty() else ServiceFactory.get(pageTitle.wikiSite).getImageInfo(titleList.joinToString("|"), pageTitle.wikiSite.languageCode)
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { binding.linkPreviewProgress.visibility = View.GONE }
                    .subscribe({ response ->
                        val pageList = response.query?.pages()?.filter { it.imageInfo() != null }.orEmpty()
                        binding.linkPreviewThumbnailGallery.setGalleryList(pageList)
                        binding.linkPreviewThumbnailGallery.listener = galleryViewListener
                    }) { caught ->
                        L.w("Failed to fetch gallery collection.", caught)
                    })
        } else {
            binding.linkPreviewProgress.visibility = View.GONE
        }
    }

    private fun showPreview(contents: LinkPreviewContents) {
        loadGallery()
        setPreviewContents(contents)
    }

    private fun showError(caught: Throwable?) {
        binding.dialogLinkPreviewContainer.layoutTransition = null
        binding.dialogLinkPreviewContainer.minimumHeight = 0
        binding.linkPreviewProgress.visibility = View.GONE
        binding.dialogLinkPreviewContentContainer.visibility = View.GONE
        binding.dialogLinkPreviewErrorContainer.visibility = View.VISIBLE
        binding.dialogLinkPreviewErrorContainer.setError(caught)
        binding.dialogLinkPreviewErrorContainer.callback = this
        LinkPreviewErrorType[caught].run {
            overlayView?.let {
                it.showSecondaryButton(false)
                it.showTertiaryButton(false)
                it.setPrimaryButtonText(resources.getString(buttonText))
                it.callback = buttonAction(binding.dialogLinkPreviewErrorContainer)
                if (this !== LinkPreviewErrorType.OFFLINE) {
                    binding.linkPreviewToolbar.setOnClickListener(null)
                    binding.linkPreviewOverflowButton.visibility = View.GONE
                }
            }
        }
    }

    private fun setPreviewContents(contents: LinkPreviewContents) {
        if (!contents.extract.isNullOrEmpty()) {
            binding.linkPreviewExtract.text = StringUtil.fromHtml(contents.extract)
        }
        contents.title.thumbUrl?.let {
            binding.linkPreviewThumbnail.visibility = View.VISIBLE
            ViewUtil.loadImage(binding.linkPreviewThumbnail, it)
        }
        overlayView?.run {
            setPrimaryButtonText(L10nUtil.getStringForArticleLanguage(pageTitle,
                    if (contents.isDisambiguation) R.string.button_continue_to_disambiguation
                    else if (pageTitle.namespace() === Namespace.TALK || pageTitle.namespace() === Namespace.USER_TALK) R.string.button_continue_to_talk_page
                    else R.string.button_continue_to_article))
        }
    }

    private fun goToExternalMapsApp() {
        location?.let {
            dismiss()
            GeoUtil.sendGeoIntent(requireActivity(), it, pageTitle.displayText)
        }
    }

    private fun goToLinkedPage(inNewTab: Boolean) {
        navigateSuccess = true
        funnel.logNavigate()
        dialog?.dismiss()
        loadPage(pageTitle, historyEntry, inNewTab)
    }

    private fun loadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        callback()?.onLinkPreviewLoadPage(title, entry, inNewTab)
    }

    private inner class OverlayViewCallback : LinkPreviewOverlayView.Callback {
        override fun onPrimaryClick() {
            goToLinkedPage(false)
        }

        override fun onSecondaryClick() {
            goToLinkedPage(true)
        }

        override fun onTertiaryClick() {
            goToExternalMapsApp()
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    companion object {
        private const val ARG_ENTRY = "entry"
        private const val ARG_LOCATION = "location"

        @JvmStatic
        fun newInstance(entry: HistoryEntry, location: Location?): LinkPreviewDialog {
            return LinkPreviewDialog().apply {
                arguments = bundleOf(ARG_ENTRY to entry, ARG_LOCATION to location)
            }
        }
    }
}
