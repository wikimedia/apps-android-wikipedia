package org.wikipedia.page.linkpreview

import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.eventplatform.ArticleLinkPreviewInteractionEvent
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.analytics.metricsplatform.ArticleLinkPreviewInteraction
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.databinding.DialogLinkPreviewBinding
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.edit.EditHandler
import org.wikipedia.edit.EditSectionActivity
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.GalleryThumbnailScrollView.GalleryViewListener
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.places.PlacesActivity
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ViewUtil
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog
import java.util.Locale

class LinkPreviewDialog : Fragment(), LinkPreviewErrorView.Callback {
    interface LoadPageCallback {
        fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean)
    }

    interface DismissCallback {
        fun onLinkPreviewDismiss()
    }

    private var _binding: DialogLinkPreviewBinding? = null
    private val binding get() = _binding!!

    private val loadPageCallback get() = getCallback(this, LoadPageCallback::class.java)
    private val dismissCallback get() = getCallback(this, DismissCallback::class.java)

    private var articleLinkPreviewInteractionEvent: ArticleLinkPreviewInteractionEvent? = null
    private var linkPreviewInteraction: ArticleLinkPreviewInteraction? = null
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
            R.id.menu_link_preview_watch -> {
                sendPlacesEvent("watch_click", "detail_overflow_menu")
                viewModel.watchOrUnwatch(viewModel.isWatched)
                true
            }
            R.id.menu_link_preview_open_in_new_tab -> {
                sendPlacesEvent("new_tab_click", "detail_overflow_menu")
                goToLinkedPage(true)
                true
            }
            R.id.menu_link_preview_copy_link -> {
                sendPlacesEvent("copy_link_click", "detail_overflow_menu")
                ClipboardUtil.setPlainText(requireActivity(), text = viewModel.pageTitle.uri)
                FeedbackUtil.showMessage(requireActivity(), R.string.address_copied)
                hide()
                true
            }
            R.id.menu_link_preview_view_on_map -> {
                PlacesEvent.logAction("places_click", "article_preview_more_menu")
                viewModel.location?.let {
                    startActivity(PlacesActivity.newIntent(requireContext(), viewModel.pageTitle, it))
                }
                hide()
                true
            }
            R.id.menu_link_preview_get_directions -> {
                sendPlacesEvent("directions_click", "detail_overflow_menu")
                viewModel.location?.let {
                    GeoUtil.sendGeoIntent(requireActivity(), it, StringUtil.fromHtml(viewModel.pageTitle.displayText).toString())
                }
                true
            }
            else -> false
        }
    }

    private fun sendPlacesEvent(action: String, activeInterface: String) {
        if (viewModel.historyEntry.source == HistoryEntry.SOURCE_PLACES) {
            PlacesEvent.logAction(action, activeInterface)
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
            imageName, viewModel.pageTitle.wikiSite, revision), options)
    }

    private val requestGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED && it.data != null) {
            startActivity(it.data!!)
        }
    }

    private val requestStubArticleEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            overlayView?.let { overlay ->
                FeedbackUtil.makeSnackbar(overlay.rootView, getString(R.string.stub_article_edit_saved_successfully))
                    .setAnchorView(overlay.secondaryButtonView).show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLinkPreviewBinding.inflate(inflater, container, false)
        binding.linkPreviewToolbar.setOnClickListener { goToLinkedPage(false) }
        binding.linkPreviewOverflowButton.setOnClickListener {
            setupOverflowMenu()
        }
        binding.linkPreviewEditButton.setOnClickListener {
            viewModel.pageTitle.run {
                requestStubArticleEditLauncher.launch(EditSectionActivity.newIntent(requireContext(), -1, null, this, Constants.InvokeSource.LINK_PREVIEW_MENU, null))
            }
        }
        L10nUtil.setConditionalLayoutDirection(binding.root, viewModel.pageTitle.wikiSite.languageCode)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is LinkPreviewViewState.Loading -> {
                            binding.linkPreviewProgress.isVisible = true
                        }
                        is LinkPreviewViewState.Error -> {
                            binding.linkPreviewProgress.isVisible = false
                            renderErrorState(it.throwable)
                        }
                        is LinkPreviewViewState.Content -> {
                            binding.linkPreviewProgress.isVisible = false
                            renderContentState(it.data)
                        }
                        is LinkPreviewViewState.Gallery -> {
                            binding.linkPreviewProgress.isVisible = false
                            renderGalleryState(it)
                        }
                        is LinkPreviewViewState.Watch -> {
                            showWatchlistSnackbar(requireActivity() as BaseActivity, viewModel.pageTitle)
                            dismiss()
                        }
                        is LinkPreviewViewState.Completed -> {
                        }
                    }
                }
            }
        }

        ((binding.bottomSheetCoordinatorLayout.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? BottomSheetBehavior)?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        return binding.root
    }

    private fun reloadContent(historyEntry: HistoryEntry) {
        viewModel.reloadContent(historyEntry)
    }

    private fun hide() {
        L.d(">>>> setting state")
        ((binding.bottomSheetCoordinatorLayout.layoutParams as? CoordinatorLayout.LayoutParams)?.behavior as? BottomSheetBehavior)?.let {
            if (it.state != BottomSheetBehavior.STATE_HIDDEN && it.state != BottomSheetBehavior.STATE_SETTLING) {
                L.d(">>>> for real")
                it.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    private fun dismiss() {
        parentFragmentManager.commit {
            remove(this@LinkPreviewDialog)
        }
        dismissCallback?.onLinkPreviewDismiss()
        if (!navigateSuccess) {
            articleLinkPreviewInteractionEvent?.logCancel()
            linkPreviewInteraction?.logCancel()
        }
    }

    private fun setupOverflowMenu() {
        val popupMenu = PopupMenu(requireActivity(), binding.linkPreviewOverflowButton)
        popupMenu.inflate(R.menu.menu_link_preview)
        popupMenu.menu.findItem(R.id.menu_link_preview_add_to_list).isVisible = !viewModel.fromPlaces
        popupMenu.menu.findItem(R.id.menu_link_preview_share_page).isVisible = !viewModel.fromPlaces
        popupMenu.menu.findItem(R.id.menu_link_preview_watch).isVisible = viewModel.fromPlaces
        popupMenu.menu.findItem(R.id.menu_link_preview_watch).title = getString(if (viewModel.isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
        popupMenu.menu.findItem(R.id.menu_link_preview_open_in_new_tab).isVisible = viewModel.fromPlaces
        popupMenu.menu.findItem(R.id.menu_link_preview_view_on_map).isVisible = !viewModel.fromPlaces && viewModel.location != null
        popupMenu.menu.findItem(R.id.menu_link_preview_get_directions).isVisible = viewModel.fromPlaces
        popupMenu.setOnMenuItemClickListener(menuListener)
        popupMenu.show()
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
            summary,
            viewModel.historyEntry.source
        )
        linkPreviewInteraction?.logLinkClick()

        revision = summary.revision

        binding.linkPreviewTitle.text = StringUtil.fromHtml(summary.displayTitle)
        if (viewModel.fromPlaces) {
            viewModel.location?.let { startLocation ->
                viewModel.lastKnownLocation?.let { endLocation ->
                    binding.linkPreviewDistance.isVisible = true
                    binding.linkPreviewDistance.text = GeoUtil.getDistanceWithUnit(startLocation, endLocation, Locale.getDefault())
                }
            }
        }
        showPreview(LinkPreviewContents(summary, viewModel.pageTitle.wikiSite))
    }

    private fun renderErrorState(throwable: Throwable) {
        L.e(throwable)
        binding.linkPreviewTitle.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)
        binding.linkPreviewThumbnail.isVisible = false
        showError(throwable)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.fromPlaces) {
            binding.overlayView.callback = OverlayViewPlacesCallback()
            binding.overlayView.setPrimaryButtonText(
                L10nUtil.getStringForArticleLanguage(viewModel.pageTitle, R.string.link_preview_dialog_share_button)
            )
            binding.overlayView.setSecondaryButtonText(
                L10nUtil.getStringForArticleLanguage(viewModel.pageTitle, R.string.link_preview_dialog_save_button)
            )
            binding.overlayView.setTertiaryButtonText(
                L10nUtil.getStringForArticleLanguage(viewModel.pageTitle, R.string.link_preview_dialog_read_button)
            )
        } else {
            binding.overlayView.callback = OverlayViewCallback()
            binding.overlayView.setPrimaryButtonText(
                L10nUtil.getStringForArticleLanguage(viewModel.pageTitle,
                    if (viewModel.pageTitle.namespace() === Namespace.TALK || viewModel.pageTitle.namespace() === Namespace.USER_TALK) R.string.button_continue_to_talk_page else R.string.button_continue_to_article
                )
            )
            binding.overlayView.setSecondaryButtonText(
                L10nUtil.getStringForArticleLanguage(viewModel.pageTitle, R.string.menu_long_press_open_in_new_tab)
            )
            binding.overlayView.showTertiaryButton(false)
        }
    }

    override fun onDestroyView() {
        binding.linkPreviewThumbnailGallery.listener = null
        binding.linkPreviewToolbar.setOnClickListener(null)
        binding.linkPreviewOverflowButton.setOnClickListener(null)
        binding.overlayView.callback = null
        _binding = null
        super.onDestroyView()
    }

    override fun onAddToList() {
        doAddToList()
    }

    override fun onDismiss() {
        dismiss()
    }

    private fun showWatchlistSnackbar(activity: AppCompatActivity, pageTitle: PageTitle) {
        viewModel.pageTitle.let {
            if (!viewModel.isWatched) {
                FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, it.displayText))
            } else if (viewModel.isWatched) {
                val snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                getString(R.string.watchlist_page_add_to_watchlist_snackbar, it.displayText, getString(WatchlistExpiry.NEVER.stringId)))
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                        ExclusiveBottomSheetPresenter.show(activity.supportFragmentManager, WatchlistExpiryDialog.newInstance(pageTitle, WatchlistExpiry.NEVER))
                }
                snackbar.show()
            }
        }
    }

    private fun doAddToList() {
        ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), viewModel.pageTitle, true, Constants.InvokeSource.LINK_PREVIEW_MENU)
        dismiss()
    }

    private fun showReadingListPopupMenu(anchorView: View) {
        if (viewModel.isInReadingList) {
            LongPressMenu(anchorView, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), viewModel.pageTitle, addToDefault, Constants.InvokeSource.LINK_PREVIEW_MENU)
                    dismiss()
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    page?.let { readingListPage ->
                        ReadingListBehaviorsUtil.moveToList(requireActivity(), readingListPage.listId, viewModel.pageTitle, Constants.InvokeSource.LINK_PREVIEW_MENU)
                    }
                    dismiss()
                }

                override fun onRemoveRequest() {
                    dismiss()
                }
            }).show(HistoryEntry(viewModel.pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK))
        } else {
            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), viewModel.pageTitle, true, Constants.InvokeSource.LINK_PREVIEW_MENU)
            dismiss()
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
            binding.overlayView.showSecondaryButton(false)
            binding.overlayView.showTertiaryButton(false)
            binding.overlayView.setPrimaryButtonText(resources.getString(buttonText))
            binding.overlayView.callback = buttonAction(binding.dialogLinkPreviewErrorContainer)
            if (this !== LinkPreviewErrorType.OFFLINE) {
                binding.linkPreviewToolbar.setOnClickListener(null)
                binding.linkPreviewOverflowButton.visibility = View.GONE
            }
        }
    }

    private fun setPreviewContents(contents: LinkPreviewContents) {
        binding.linkPreviewExtractWebview.setBackgroundColor(Color.TRANSPARENT)
        val colorHex = ResourceUtil.colorToCssString(
            ResourceUtil.getThemedColor(
                requireContext(),
                android.R.attr.textColorPrimary
            )
        )
        val dir = if (L10nUtil.isLangRTL(viewModel.pageTitle.wikiSite.languageCode)) "rtl" else "ltr"
        val editVisibility = contents.extract.isNullOrBlank() && contents.ns?.id == Namespace.MAIN.code()
        binding.linkPreviewEditButton.isVisible = editVisibility
        binding.linkPreviewThumbnailGallery.isVisible = !editVisibility
        val extract = if (editVisibility) "<i>" + getString(R.string.link_preview_stub_placeholder_text) + "</i>" else contents.extract
        binding.linkPreviewExtractWebview.loadDataWithBaseURL(
            null,
            "${JavaScriptActionHandler.getCssStyles(viewModel.pageTitle.wikiSite)}<div style=\"line-height: 150%; color: #$colorHex\" dir=\"$dir\">$extract</div>",
            "text/html",
            "UTF-8",
            null
        )
        contents.title.thumbUrl?.let {
            binding.linkPreviewThumbnail.visibility = View.VISIBLE
            ViewUtil.loadImage(binding.linkPreviewThumbnail, contents.title.thumbUrl)
        }
        if (!viewModel.fromPlaces) {
            binding.overlayView.setPrimaryButtonText(
                L10nUtil.getStringForArticleLanguage(
                    viewModel.pageTitle,
                    if (contents.isDisambiguation) R.string.button_continue_to_disambiguation
                    else if (viewModel.pageTitle.namespace() === Namespace.TALK || viewModel.pageTitle.namespace() === Namespace.USER_TALK) R.string.button_continue_to_talk_page
                    else R.string.button_continue_to_article
                )
            )
        } else if (viewModel.fromPlaces) {
            binding.overlayView.setSecondaryButtonText(L10nUtil.getStringForArticleLanguage(viewModel.pageTitle,
                if (viewModel.isInReadingList) R.string.link_preview_dialog_saved_button else R.string.link_preview_dialog_save_button))
        }
    }

    private fun goToLinkedPage(inNewTab: Boolean) {
        navigateSuccess = true
        articleLinkPreviewInteractionEvent?.logNavigate()
        linkPreviewInteraction?.logNavigate()
        dismiss()
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
            // ignore
        }
    }

    private inner class OverlayViewPlacesCallback : LinkPreviewOverlayView.Callback {
        override fun onPrimaryClick() {
            sendPlacesEvent("share_click", "detail_toolbar")
            ShareUtil.shareText(requireContext(), viewModel.pageTitle)
        }

        override fun onSecondaryClick() {
            sendPlacesEvent("save_click", "detail_toolbar")
            showReadingListPopupMenu(binding.overlayView.secondaryButtonView)
        }

        override fun onTertiaryClick() {
            sendPlacesEvent("read_click", "detail_toolbar")
            goToLinkedPage(false)
        }
    }

    companion object {
        const val FRAGMENT_TAG = "linkPreviewDialog"
        const val ARG_ENTRY = "entry"
        const val ARG_LOCATION = "location"
        const val ARG_LAST_KNOWN_LOCATION = "lastKnownLocation"

        fun newInstance(entry: HistoryEntry, location: Location? = null, lastKnownLocation: Location? = null): LinkPreviewDialog {
            return LinkPreviewDialog().apply {
                arguments = bundleOf(
                    ARG_ENTRY to entry,
                    ARG_LOCATION to location,
                    ARG_LAST_KNOWN_LOCATION to lastKnownLocation
                )
            }
        }

        fun show(fragmentManager: FragmentManager, resId: Int, entry: HistoryEntry, location: Location? = null, lastKnownLocation: Location? = null) {
            val existing = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? LinkPreviewDialog
            if (existing != null) {
                existing.reloadContent(entry)
                return
            }

            fragmentManager.commit {
                add(resId, newInstance(entry, location, lastKnownLocation), FRAGMENT_TAG)
            }
        }

        fun hide(fragmentManager: FragmentManager) {
            val existing = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? LinkPreviewDialog
            existing?.hide()
        }
    }
}
