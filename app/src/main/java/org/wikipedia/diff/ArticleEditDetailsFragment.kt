package org.wikipedia.diff

import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentArticleEditDetailsBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.watch.Watch
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog

class ArticleEditDetailsFragment : Fragment(), WatchlistExpiryDialog.Callback, LinkPreviewDialog.Callback, MenuProvider {
    private var _binding: FragmentArticleEditDetailsBinding? = null
    private val binding get() = _binding!!

    private var isWatched = false
    private var hasWatchlistExpiry = false

    private val viewModel: ArticleEditDetailsViewModel by viewModels { ArticleEditDetailsViewModel.Factory(requireArguments()) }
    private var editHistoryInteractionEvent: EditHistoryInteractionEvent? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentArticleEditDetailsBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.diffRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        FeedbackUtil.setButtonLongPressToast(binding.newerIdButton, binding.olderIdButton)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
        setLoadingState()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.articleTitleView.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)

        viewModel.watchedStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                if (editHistoryInteractionEvent == null) {
                    editHistoryInteractionEvent = EditHistoryInteractionEvent(viewModel.pageTitle.wikiSite.dbName(), viewModel.pageId)
                    editHistoryInteractionEvent?.logRevision()
                }
                isWatched = it.data.watched
                hasWatchlistExpiry = it.data.hasWatchlistExpiry()
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.revisionDetails.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                updateDiffCharCountView(viewModel.diffSize)
                updateAfterRevisionFetchSuccess()
                updateUndoAndRollbackButtons()
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.diffText.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                binding.diffRecyclerView.adapter = DiffUtil.DiffLinesAdapter(DiffUtil.buildDiffLinesList(requireContext(), it.data.diff))
                updateAfterDiffFetchSuccess()
                binding.progressBar.isVisible = false
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.singleRevisionText.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                binding.diffRecyclerView.adapter = DiffUtil.DiffLinesAdapter(DiffUtil.buildDiffLinesList(requireContext(), it.data))
                updateAfterDiffFetchSuccess()
                binding.progressBar.isVisible = false
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.thankStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                FeedbackUtil.showMessage(requireActivity(), getString(R.string.thank_success_message,
                        viewModel.revisionTo?.user))
                setButtonTextAndIconColor(binding.thankButton, ResourceUtil.getThemedColor(requireContext(),
                        R.attr.placeholder_color))
                binding.thankButton.isEnabled = false
                editHistoryInteractionEvent?.logThankSuccess()
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
                editHistoryInteractionEvent?.logThankFail()
            }
        }

        viewModel.watchResponse.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                val firstWatch = it.data.getFirst()
                if (firstWatch != null) {
                    showWatchlistSnackbar(viewModel.lastWatchExpiry, firstWatch)
                }
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.undoEditResponse.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                setLoadingState()
                viewModel.getRevisionDetails(it.data.edit!!.newRevId)
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.revision_undo_success)).show()
                editHistoryInteractionEvent?.logUndoSuccess()
            } else if (it is Resource.Error) {
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
                editHistoryInteractionEvent?.logUndoFail()
            }
        }

        viewModel.rollbackRights.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                updateUndoAndRollbackButtons()
            } else if (it is Resource.Error) {
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }

        viewModel.rollbackResponse.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                setLoadingState()
                viewModel.getRevisionDetails(it.data.rollback?.revision ?: 0)
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.revision_rollback_success), FeedbackUtil.LENGTH_DEFAULT).show()
            } else if (it is Resource.Error) {
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }

        L10nUtil.setConditionalLayoutDirection(requireView(), viewModel.pageTitle.wikiSite.languageCode)

        binding.scrollContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val bounds = Rect()
            binding.contentContainer.offsetDescendantRectToMyCoords(binding.articleTitleDivider, bounds)
            if (scrollY > bounds.top) {
                binding.overlayRevisionDetailsView.visibility = View.VISIBLE
                (requireActivity() as AppCompatActivity).supportActionBar?.let {
                    if (it.title.isNullOrEmpty()) {
                        it.title = getString(R.string.revision_diff_compare_title, StringUtil.fromHtml(viewModel.pageTitle.displayText))
                    }
                    it.setDisplayShowTitleEnabled(true)
                }
            } else {
                binding.overlayRevisionDetailsView.visibility = View.INVISIBLE
                (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
            }
        })
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setUpListeners() {
        binding.articleTitleView.setOnClickListener {
            if (viewModel.pageTitle.namespace() == Namespace.USER_TALK || viewModel.pageTitle.namespace() == Namespace.TALK) {
                startActivity(TalkTopicsActivity.newIntent(requireContext(), viewModel.pageTitle, InvokeSource.DIFF_ACTIVITY))
            } else if (viewModel.pageTitle.namespace() == Namespace.FILE) {
                startActivity(FilePageActivity.newIntent(requireContext(), viewModel.pageTitle))
            } else {
                ExclusiveBottomSheetPresenter.show(childFragmentManager, LinkPreviewDialog.newInstance(
                        HistoryEntry(viewModel.pageTitle, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS), null))
            }
        }
        binding.newerIdButton.setOnClickListener {
            setLoadingState()
            viewModel.goForward()
            editHistoryInteractionEvent?.logNewerEditChevronClick()
        }
        binding.olderIdButton.setOnClickListener {
            setLoadingState()
            viewModel.goBackward()
            editHistoryInteractionEvent?.logOlderEditChevronClick()
        }

        binding.usernameFromButton.setOnClickListener {
            showUserPopupMenu(viewModel.revisionFrom, binding.usernameFromButton)
        }

        binding.usernameToButton.setOnClickListener {
            showUserPopupMenu(viewModel.revisionTo, binding.usernameToButton)
        }

        binding.thankButton.setOnClickListener {
            showThankDialog()
            editHistoryInteractionEvent?.logThankTry()
        }

        binding.undoButton.setOnClickListener {
            showUndoDialog()
            editHistoryInteractionEvent?.logUndoTry()
        }

        binding.rollbackButton.setOnClickListener {
            showRollbackDialog()
        }

        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        val watchlistItem = menu.findItem(R.id.menu_add_watchlist)
        watchlistItem.isVisible = AccountUtil.isLoggedIn
        watchlistItem.title = getString(if (isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
        watchlistItem.setIcon(getWatchlistIcon(isWatched, hasWatchlistExpiry))
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                ShareUtil.shareText(requireContext(), StringUtil.fromHtml(viewModel.pageTitle.displayText).toString(), getSharableDiffUrl())
                editHistoryInteractionEvent?.logShareClick()
                true
            }
            R.id.menu_add_watchlist -> {
                viewModel.watchOrUnwatch(isWatched, WatchlistExpiry.NEVER, isWatched)
                if (isWatched) editHistoryInteractionEvent?.logUnwatchClick() else editHistoryInteractionEvent?.logWatchClick()
                true
            }
            R.id.menu_copy_link_to_clipboard -> {
                copyLink(getSharableDiffUrl())
                true
            }
            else -> false
        }
    }

    private fun showUserPopupMenu(revision: Revision?, anchorView: View) {
        revision?.let {
            UserTalkPopupHelper.show(requireActivity() as AppCompatActivity,
                    PageTitle(UserAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode),
                            it.user, viewModel.pageTitle.wikiSite), it.isAnon, anchorView,
                    InvokeSource.DIFF_ACTIVITY, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS)
        }
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)
        binding.errorView.setError(t)
        binding.errorView.isVisible = true
        binding.revisionDetailsView.isVisible = false
        binding.progressBar.isVisible = false
    }

    private fun updateDiffCharCountView(diffSize: Int) {
        binding.diffCharacterCountView.text = StringUtil.getDiffBytesText(requireContext(), diffSize)
        if (diffSize >= 0) {
            val diffColor = if (diffSize > 0) R.attr.success_color else R.attr.secondary_color
            binding.diffCharacterCountView.setTextColor(ResourceUtil.getThemedColor(requireContext(), diffColor))
        } else {
            binding.diffCharacterCountView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.destructive_color))
        }
    }

    private fun setLoadingState() {
        binding.progressBar.isVisible = true
        binding.revisionDetailsView.isVisible = false
        binding.diffRecyclerView.isVisible = false
    }

    private fun updateAfterRevisionFetchSuccess() {
        if (viewModel.revisionFrom != null) {
            binding.usernameFromButton.text = viewModel.revisionFrom!!.user
            binding.revisionFromTimestamp.text = DateUtil.getTimeAndDateString(requireContext(), viewModel.revisionFrom!!.timeStamp)
            binding.revisionFromEditComment.text = StringUtil.fromHtml(viewModel.revisionFrom!!.parsedcomment.trim())
            binding.revisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color))
            binding.overlayRevisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color))
            binding.usernameFromButton.isVisible = true
            binding.revisionFromEditComment.isVisible = true
            binding.undoButton.isVisible = true
        } else {
            binding.usernameFromButton.isVisible = false
            binding.revisionFromEditComment.isVisible = false
            binding.revisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.placeholder_color))
            binding.overlayRevisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.placeholder_color))
            binding.revisionFromTimestamp.text = getString(R.string.revision_initial_none)
            binding.undoButton.isVisible = false
        }
        binding.overlayRevisionFromTimestamp.text = binding.revisionFromTimestamp.text

        viewModel.revisionTo?.let {
            binding.usernameToButton.text = it.user
            binding.revisionToTimestamp.text = DateUtil.getTimeAndDateString(requireContext(), it.timeStamp)
            binding.overlayRevisionToTimestamp.text = binding.revisionToTimestamp.text
            binding.revisionToEditComment.text = StringUtil.fromHtml(it.parsedcomment.trim())
        }

        setEnableDisableTint(binding.newerIdButton, !viewModel.canGoForward)
        setEnableDisableTint(binding.olderIdButton, viewModel.revisionFromId == 0L)
        binding.newerIdButton.isEnabled = viewModel.canGoForward
        binding.olderIdButton.isEnabled = viewModel.revisionFromId != 0L

        setButtonTextAndIconColor(binding.thankButton, ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color))

        binding.thankButton.isEnabled = true
        binding.thankButton.isVisible = AccountUtil.isLoggedIn &&
                AccountUtil.userName != viewModel.revisionTo?.user &&
                viewModel.revisionTo?.isAnon == false
        binding.revisionDetailsView.isVisible = true
        binding.errorView.isVisible = false
    }

    private fun updateAfterDiffFetchSuccess() {
        binding.diffRecyclerView.isVisible = true
    }

    private fun setEnableDisableTint(view: ImageView, isDisabled: Boolean) {
        ImageViewCompat.setImageTintList(view, AppCompatResources.getColorStateList(requireContext(),
            ResourceUtil.getThemedAttributeId(requireContext(), if (isDisabled)
                R.attr.inactive_color else R.attr.secondary_color)))
    }

    private fun setButtonTextAndIconColor(view: MaterialButton, themedColor: Int) {
        view.setTextColor(themedColor)
        view.iconTint = ColorStateList.valueOf(themedColor)
    }

    @DrawableRes
    private fun getWatchlistIcon(isWatched: Boolean, hasWatchlistExpiry: Boolean): Int {
        return if (isWatched && !hasWatchlistExpiry) {
            R.drawable.ic_star_24
        } else if (!isWatched) {
            R.drawable.ic_baseline_star_outline_24
        } else {
            R.drawable.ic_baseline_star_half_24
        }
    }

    private fun showWatchlistSnackbar(expiry: WatchlistExpiry, watch: Watch) {
        isWatched = watch.watched
        hasWatchlistExpiry = expiry != WatchlistExpiry.NEVER
        if (watch.unwatched) {
            FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, viewModel.pageTitle.displayText))
        } else if (watch.watched) {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                    getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                            viewModel.pageTitle.displayText,
                            getString(expiry.stringId)))
            if (!viewModel.watchlistExpiryChanged) {
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                    viewModel.watchlistExpiryChanged = true
                    ExclusiveBottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(expiry))
                }
            }
            snackbar.show()
        }
    }

    private fun showThankDialog() {
        val parent = FrameLayout(requireContext())
        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setView(parent)
                .setPositiveButton(R.string.thank_dialog_positive_button_text) { _, _ ->
                    viewModel.sendThanks(viewModel.pageTitle.wikiSite, viewModel.revisionToId)
                }
                .setNegativeButton(R.string.thank_dialog_negative_button_text) { _, _ ->
                    editHistoryInteractionEvent?.logThankCancel()
                }
                .create()
        dialog.layoutInflater.inflate(R.layout.view_thank_dialog, parent)
        dialog.show()
    }

    private fun showUndoDialog() {
        val dialog = UndoEditDialog(editHistoryInteractionEvent, requireActivity()) { text ->
            viewModel.revisionTo?.let {
                binding.progressBar.isVisible = true
                viewModel.undoEdit(viewModel.pageTitle, it.user, text.toString(), viewModel.revisionToId, 0)
            }
        }
        dialog.show()
    }

    private fun showRollbackDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.revision_rollback_dialog_title)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                binding.progressBar.isVisible = true
                viewModel.revisionTo?.let {
                    viewModel.postRollback(viewModel.pageTitle, it.user)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateUndoAndRollbackButtons() {
        binding.rollbackButton.isVisible = AccountUtil.isLoggedIn && viewModel.hasRollbackRights && !viewModel.canGoForward
        binding.undoButton.isVisible = AccountUtil.isLoggedIn
    }

    private fun getSharableDiffUrl(): String {
        return viewModel.pageTitle.getWebApiUrl("diff=${viewModel.revisionToId}&oldid=${viewModel.revisionFromId}&variant=${viewModel.pageTitle.wikiSite.languageCode}")
    }

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        viewModel.watchOrUnwatch(isWatched, expiry, false)
        ExclusiveBottomSheetPresenter.dismiss(childFragmentManager)
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        if (inNewTab) {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
        } else {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
        }
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        copyLink(title.uri)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.show(childFragmentManager,
                AddToReadingListDialog.newInstance(title, InvokeSource.LINK_PREVIEW_MENU))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(requireContext(), title)
    }

    private fun copyLink(uri: String?) {
        ClipboardUtil.setPlainText(requireContext(), text = uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    companion object {
        fun newInstance(title: PageTitle, pageId: Int, revisionFrom: Long, revisionTo: Long): ArticleEditDetailsFragment {
            return ArticleEditDetailsFragment().apply {
                arguments = bundleOf(ArticleEditDetailsActivity.EXTRA_ARTICLE_TITLE to title,
                        ArticleEditDetailsActivity.EXTRA_PAGE_ID to pageId,
                        ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_FROM to revisionFrom,
                        ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_TO to revisionTo)
            }
        }
    }
}
