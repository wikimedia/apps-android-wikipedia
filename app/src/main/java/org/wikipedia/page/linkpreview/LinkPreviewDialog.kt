package org.wikipedia.page.linkpreview

import android.content.DialogInterface
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.eventplatform.ArticleLinkPreviewInteractionEvent
import org.wikipedia.analytics.metricsplatform.ArticleLinkPreviewInteraction
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.DialogLinkPreviewBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.GalleryThumbnailScrollView.GalleryViewListener
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewUtil

class LinkPreviewDialog : ExtendedBottomSheetDialogFragment(), LinkPreviewErrorView.Callback, DialogInterface.OnDismissListener {
    interface LoadPageCallback {
        fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean)
    }

    interface AddToListCallback {
        fun onLinkPreviewAddToList(title: PageTitle)
    }

    private var _binding: DialogLinkPreviewBinding? = null
    private val binding get() = _binding!!

    private val loadPageCallback get() = getCallback(this, LoadPageCallback::class.java)
    private val addToListCallback get() = getCallback(this, AddToListCallback::class.java)

    private var articleLinkPreviewInteractionEvent: ArticleLinkPreviewInteractionEvent? = null
    private var linkPreviewInteraction: ArticleLinkPreviewInteraction? = null
    private var overlayView: LinkPreviewOverlayView? = null
    private var navigateSuccess = false
    private var revision: Long = 0
    private val viewModel: LinkPreviewViewModel by viewModels { LinkPreviewViewModel.Factory(requireArguments()) }

    private val menuListener = PopupMenu.OnMenuItemClickListener { item ->
        return@OnMenuItemClickListener when (item.itemId) {
            R.id.menu_link_preview_add_to_list -> {
                doAddToList()
                true
            }
            R.id.menu_link_preview_share_page -> {
                ShareUtil.shareText(requireContext(), viewModel.pageTitle)
                true
            }
            R.id.menu_link_preview_copy_link -> {
                ClipboardUtil.setPlainText(requireActivity(), text = viewModel.pageTitle.uri)
                FeedbackUtil.showMessage(requireActivity(), R.string.address_copied)
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
        requestGalleryLauncher.launch(GalleryActivity.newIntent(requireContext(), viewModel.pageTitle,
            imageName, viewModel.pageTitle.wikiSite, revision, GalleryActivity.SOURCE_LINK_PREVIEW), options)
    }

    private val requestGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED && it.data != null) {
            startActivity(it.data!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLinkPreviewBinding.inflate(inflater, container, false)
        binding.linkPreviewToolbar.setOnClickListener { goToLinkedPage(false) }
        binding.linkPreviewOverflowButton.setOnClickListener {
            PopupMenu(requireActivity(), binding.linkPreviewOverflowButton).run {
                inflate(R.menu.menu_link_preview)
                setOnMenuItemClickListener(menuListener)
                show()
            }
        }
        L10nUtil.setConditionalLayoutDirection(binding.root, viewModel.pageTitle.wikiSite.languageCode)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is LinkPreviewViewState.Loading -> {
                            binding.linkPreviewProgress.visibility = View.VISIBLE
                        }
                        is LinkPreviewViewState.Error -> {
                            renderErrorState(it.throwable)
                        }
                        is LinkPreviewViewState.Content -> {
                            renderContentState(it.data)
                        }
                        is LinkPreviewViewState.Gallery -> {
                            renderGalleryState(it)
                        }
                        is LinkPreviewViewState.Completed -> {
                            binding.linkPreviewProgress.visibility = View.GONE
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun renderGalleryState(it: LinkPreviewViewState.Gallery) {
        binding.linkPreviewThumbnailGallery.setGalleryList(it.data)
        binding.linkPreviewThumbnailGallery.listener = galleryViewListener
        binding.linkPreviewProgress.visibility = View.GONE
    }

    private fun renderContentState(summary: PageSummary) {
        articleLinkPreviewInteractionEvent = ArticleLinkPreviewInteractionEvent(
                viewModel.pageTitle.wikiSite.dbName(),
                summary.pageId,
                viewModel.historyEntry.source
        )
        articleLinkPreviewInteractionEvent?.logLinkClick()

        linkPreviewInteraction = ArticleLinkPreviewInteraction(
            viewModel.pageTitle,
            summary.pageId,
            viewModel.historyEntry.source
        )
        linkPreviewInteraction?.logLinkClick()

        revision = summary.revision

        binding.linkPreviewTitle.text = StringUtil.fromHtml(summary.displayTitle)
        showPreview(LinkPreviewContents(summary, viewModel.pageTitle.wikiSite))
    }

    private fun renderErrorState(throwable: Throwable) {
        L.e(throwable)
        binding.linkPreviewTitle.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)
        showError(throwable)
    }

    override fun onResume() {
        super.onResume()
        val containerView = requireDialog().findViewById<ViewGroup>(R.id.container)
        if (overlayView == null && containerView != null) {
            LinkPreviewOverlayView(requireContext()).let {
                overlayView = it
                it.callback = OverlayViewCallback()
                it.setPrimaryButtonText(
                        L10nUtil.getStringForArticleLanguage(
                                viewModel.pageTitle,
                                if (viewModel.pageTitle.namespace() === Namespace.TALK || viewModel.pageTitle.namespace() === Namespace.USER_TALK) R.string.button_continue_to_talk_page else R.string.button_continue_to_article
                        )
                )
                it.setSecondaryButtonText(
                        L10nUtil.getStringForArticleLanguage(
                                viewModel.pageTitle,
                                R.string.menu_long_press_open_in_new_tab
                        )
                )
                it.showTertiaryButton(viewModel.location != null)
                containerView.addView(it)
            }
        }
    }

    override fun onDestroyView() {
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
            articleLinkPreviewInteractionEvent?.logCancel()
            linkPreviewInteraction?.logCancel()
        }
    }

    override fun onAddToList() {
        doAddToList()
    }

    override fun onDismiss() {
        dismiss()
    }

    private fun doAddToList() {
        addToListCallback.let {
            if (it != null) {
                it.onLinkPreviewAddToList(viewModel.pageTitle)
            } else {
                ExclusiveBottomSheetPresenter.showAddToListDialog(requireActivity().supportFragmentManager,
                    viewModel.pageTitle, Constants.InvokeSource.LINK_PREVIEW_MENU)
            }
        }
    }

    private fun showPreview(contents: LinkPreviewContents) {
        viewModel.loadGallery(revision)
        setPreviewContents(contents)
    }

    private fun showError(caught: Throwable?) {
        binding.dialogLinkPreviewContainer.layoutTransition = null
        binding.dialogLinkPreviewContainer.minimumHeight = 0
        binding.linkPreviewProgress.visibility = View.GONE
        binding.dialogLinkPreviewContentContainer.visibility = View.GONE
        binding.dialogLinkPreviewErrorContainer.visibility = View.VISIBLE
        binding.dialogLinkPreviewErrorContainer.callback = this
        binding.dialogLinkPreviewErrorContainer.setError(caught, viewModel.pageTitle)
        LinkPreviewErrorType[caught, viewModel.pageTitle].run {
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
            binding.linkPreviewExtractWebview.setBackgroundColor(Color.TRANSPARENT)
            val colorHex = ResourceUtil.colorToCssString(
                    ResourceUtil.getThemedColor(
                            requireContext(),
                            android.R.attr.textColorPrimary
                    )
            )
            val dir = if (L10nUtil.isLangRTL(viewModel.pageTitle.wikiSite.languageCode)) "rtl" else "ltr"
            binding.linkPreviewExtractWebview.loadDataWithBaseURL(
                    null,
                    "${JavaScriptActionHandler.getCssStyles(viewModel.pageTitle.wikiSite)}<div style=\"line-height: 150%; color: #$colorHex\" dir=\"$dir\">${contents.extract}</div>",
                    "text/html",
                    "UTF-8",
                    null
            )
        }
        contents.title.thumbUrl?.let {
            binding.linkPreviewThumbnail.visibility = View.VISIBLE
            ViewUtil.loadImage(binding.linkPreviewThumbnail, it)
        }
        overlayView?.run {
            setPrimaryButtonText(
                    L10nUtil.getStringForArticleLanguage(
                            viewModel.pageTitle,
                            if (contents.isDisambiguation) R.string.button_continue_to_disambiguation
                            else if (viewModel.pageTitle.namespace() === Namespace.TALK || viewModel.pageTitle.namespace() === Namespace.USER_TALK) R.string.button_continue_to_talk_page
                            else R.string.button_continue_to_article
                    )
            )
        }
    }

    private fun goToExternalMapsApp() {
        viewModel.location?.let {
            dismiss()
            GeoUtil.sendGeoIntent(requireActivity(), it, viewModel.pageTitle.displayText)
        }
    }

    private fun goToLinkedPage(inNewTab: Boolean) {
        navigateSuccess = true
        articleLinkPreviewInteractionEvent?.logNavigate()
        linkPreviewInteraction?.logNavigate()
        dialog?.dismiss()
        loadPage(viewModel.pageTitle, viewModel.historyEntry, inNewTab)
    }

    private fun loadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        loadPageCallback.let {
            if (it != null) {
                it.onLinkPreviewLoadPage(title, entry, inNewTab)
            } else {
                requireActivity().startActivity(
                    if (inNewTab) PageActivity.newIntentForNewTab(requireContext(), entry, entry.title)
                    else PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title, false)
                )
            }
        }
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

    companion object {
        const val ARG_ENTRY = "entry"
        const val ARG_LOCATION = "location"

        fun newInstance(entry: HistoryEntry, location: Location?): LinkPreviewDialog {
            return LinkPreviewDialog().apply {
                arguments = bundleOf(ARG_ENTRY to entry, ARG_LOCATION to location)
            }
        }
    }
}
