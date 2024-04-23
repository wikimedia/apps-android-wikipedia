package org.wikipedia.diff

import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.EditHistoryInteractionEvent
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentArticleEditDetailsBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.suggestededits.SuggestedEditsCardsFragment
import org.wikipedia.talk.TalkReplyActivity
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.talk.template.TalkTemplatesActivity
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.SurveyDialog
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog

class ArticleEditDetailsFragment : Fragment(), WatchlistExpiryDialog.Callback, MenuProvider {
    interface Callback {
        fun onUndoSuccess()
        fun onRollbackSuccess()
    }

    private var _binding: FragmentArticleEditDetailsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ArticleEditDetailsViewModel by viewModels { ArticleEditDetailsViewModel.Factory(requireArguments()) }
    private var editHistoryInteractionEvent: EditHistoryInteractionEvent? = null

    private val actionBarOffsetChangedListener =
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val bounds = Rect()
            binding.collapsingToolbarLayout.offsetDescendantRectToMyCoords(binding.articleTitleDivider, bounds)
            binding.overlayRevisionDetailsView.isVisible = -verticalOffset > bounds.top
        }

    private val requestTalk = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS || result.resultCode == TalkReplyActivity.RESULT_SAVE_TEMPLATE) {
            val pageTitle = result.data?.parcelableExtra<PageTitle>(Constants.ARG_TITLE) ?: viewModel.pageTitle
            val message = if (result.resultCode == TalkReplyActivity.RESULT_EDIT_SUCCESS) {
                PatrollerExperienceEvent.logAction("publish_message_toast", "pt_warning_messages")
                R.string.talk_warn_submitted
            } else {
                PatrollerExperienceEvent.logAction("publish_message_saved_toast", "pt_warning_messages")
                R.string.talk_warn_submitted_and_saved
            }
            FeedbackUtil.makeSnackbar(requireActivity(), getString(message))
                .setAction(R.string.patroller_tasks_patrol_edit_snackbar_view) {
                    if (isAdded) {
                        PatrollerExperienceEvent.logAction("publish_message_view_click", "pt_warning_messages")
                        startActivity(TalkTopicsActivity.newIntent(requireContext(), pageTitle, InvokeSource.DIFF_ACTIVITY))
                    }
                }
                .setAnchorView(binding.navTabContainer)
                .show()
        }
    }

    private val sequentialTooltipRunnable = Runnable {
        if (!isAdded) {
            return@Runnable
        }
        sendPatrollerExperienceEvent("impression", "pt_tooltip")
        val balloon = FeedbackUtil.getTooltip(requireContext(), getString(R.string.patroller_diff_tooltip_one), autoDismiss = true, showDismissButton = true, dismissButtonText = R.string.image_recommendation_tooltip_next, countNum = 1, countTotal = 2)
        balloon.showAlignBottom(binding.oresDamagingButton)
        balloon.relayShowAlignBottom(FeedbackUtil.getTooltip(requireContext(), getString(R.string.patroller_diff_tooltip_two), autoDismiss = true, showDismissButton = true, countNum = 2, countTotal = 2), binding.oresGoodFaithButton)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentArticleEditDetailsBinding.inflate(inflater, container, false)
        binding.diffRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        FeedbackUtil.setButtonTooltip(binding.newerIdButton, binding.olderIdButton)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
        setLoadingState()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (savedInstanceState == null) {
            EditAttemptStepEvent.logInit(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
        }

        if (!viewModel.fromRecentEdits) {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(R.string.revision_diff_compare)
            binding.articleTitleView.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)
        }

        viewModel.watchedStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                if (editHistoryInteractionEvent == null) {
                    editHistoryInteractionEvent = EditHistoryInteractionEvent(viewModel.pageTitle.wikiSite.dbName(), viewModel.pageId)
                    editHistoryInteractionEvent?.logRevision()
                }
                updateWatchButton(it.data.hasWatchlistExpiry())
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.revisionDetails.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                updateDiffCharCountView(viewModel.diffSize)
                updateAfterRevisionFetchSuccess()
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.singleRevisionText.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                binding.diffRecyclerView.adapter = DiffUtil.DiffLinesAdapter(DiffUtil.buildDiffLinesList(requireContext(), it.data))
                updateAfterDiffFetchSuccess()
                binding.progressBar.isVisible = false
                binding.navTabContainer.isVisible = true
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
        }

        viewModel.thankStatus.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                showThankSnackbar()
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
                    showWatchlistSnackbar()
                }
            } else if (it is Resource.Error) {
                setErrorState(it.throwable)
            }
            requireActivity().invalidateOptionsMenu()
        }

        viewModel.undoEditResponse.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                setLoadingState()
                viewModel.getRevisionDetails(it.data.edit!!.newRevId)
                sendPatrollerExperienceEvent("undo_success", "pt_edit",
                    PatrollerExperienceEvent.getActionDataString(it.data.edit.newRevId))
                showUndoSnackbar()
                editHistoryInteractionEvent?.logUndoSuccess()
                callback()?.onUndoSuccess()
            } else if (it is Resource.Error) {
                EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
                editHistoryInteractionEvent?.logUndoFail()
            }
        }

        viewModel.rollbackRights.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                updateActionButtons()
            } else if (it is Resource.Error) {
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }

        viewModel.rollbackResponse.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = false
            if (it is Resource.Success) {
                EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                setLoadingState()
                viewModel.getRevisionDetails(it.data.rollback?.revision ?: 0)
                sendPatrollerExperienceEvent("rollback_success", "pt_edit",
                    PatrollerExperienceEvent.getActionDataString(it.data.rollback?.revision ?: 0))
                showRollbackSnackbar()
                callback()?.onRollbackSuccess()
            } else if (it is Resource.Error) {
                EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                it.throwable.printStackTrace()
                FeedbackUtil.showError(requireActivity(), it.throwable)
            }
        }

        viewModel.diffText.observe(viewLifecycleOwner) {
            if (it is Resource.Success) {
                binding.diffRecyclerView.adapter = DiffUtil.DiffLinesAdapter(DiffUtil.buildDiffLinesList(requireContext(), it.data.diff))
                updateAfterDiffFetchSuccess()
                updateActionButtons()
                binding.progressBar.isVisible = false
                binding.navTabContainer.isVisible = true
            } else if (it is Resource.Error) {
                if (it.throwable is HttpStatusException && it.throwable.code == 403) {
                    binding.progressBar.isVisible = false
                    binding.diffRecyclerView.isVisible = false
                    binding.undoButton.isVisible = false
                    binding.thankButton.isVisible = false
                    binding.diffUnavailableContainer.isVisible = true
                } else {
                    setErrorState(it.throwable)
                }
            }
        }

        L10nUtil.setConditionalLayoutDirection(requireView(), viewModel.pageTitle.wikiSite.languageCode)

        binding.appBarLayout.addOnOffsetChangedListener(actionBarOffsetChangedListener)
    }

    override fun onDestroyView() {
        binding.appBarLayout.removeOnOffsetChangedListener(actionBarOffsetChangedListener)
        binding.root.removeCallbacks(sequentialTooltipRunnable)
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
                        HistoryEntry(viewModel.pageTitle, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS)))
            }
        }
        binding.newerIdButton.setOnClickListener {
            sendPatrollerExperienceEvent("edit_right_chevron_click", "pt_edit")
            setLoadingState()
            viewModel.goForward()
            editHistoryInteractionEvent?.logNewerEditChevronClick()
        }
        binding.olderIdButton.setOnClickListener {
            sendPatrollerExperienceEvent("edit_left_chevron_click", "pt_edit")
            setLoadingState()
            viewModel.goBackward()
            editHistoryInteractionEvent?.logOlderEditChevronClick()
        }

        binding.usernameFromButton.setOnClickListener {
            showUserPopupMenu(viewModel.revisionFrom, true, binding.usernameFromButton)
        }

        binding.usernameToButton.setOnClickListener {
            showUserPopupMenu(viewModel.revisionTo, false, binding.usernameToButton)
        }

        binding.thankButton.setOnClickListener {
            sendPatrollerExperienceEvent("thank_init", "pt_toolbar")
            showThankDialog()
            editHistoryInteractionEvent?.logThankTry()
        }

        binding.undoButton.setOnClickListener {
            val canUndo = viewModel.revisionFrom != null && AccountUtil.isLoggedIn
            val canRollback = AccountUtil.isLoggedIn && viewModel.hasRollbackRights && !viewModel.canGoForward
            EditAttemptStepEvent.logSaveIntent(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
            if (canUndo && canRollback) {
                PopupMenu(requireContext(), binding.undoLabel, Gravity.END).apply {
                    menuInflater.inflate(R.menu.menu_context_undo, menu)
                    setForceShowIcon(true)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.menu_undo -> {
                                sendPatrollerExperienceEvent("undo_menu_init", "pt_toolbar")
                                showUndoDialog()
                                editHistoryInteractionEvent?.logUndoTry()
                                true
                            }
                            R.id.menu_rollback -> {
                                sendPatrollerExperienceEvent("rollback_menu_init", "pt_toolbar")
                                showRollbackDialog()
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            } else if (canUndo) {
                sendPatrollerExperienceEvent("undo_init", "pt_toolbar")
                showUndoDialog()
                editHistoryInteractionEvent?.logUndoTry()
            }
        }

        binding.watchButton.setOnClickListener {
            sendPatrollerExperienceEvent(if (viewModel.isWatched) "unwatch_init" else "watch_init", "pt_toolbar")
            viewModel.watchOrUnwatch(viewModel.isWatched)
            if (viewModel.isWatched) editHistoryInteractionEvent?.logUnwatchClick() else editHistoryInteractionEvent?.logWatchClick()
        }
        updateWatchButton(false)

        binding.warnButton.setOnClickListener {
            sendPatrollerExperienceEvent("warn_init", "pt_toolbar")
            viewModel.revisionTo?.let { revision ->
                val pageTitle = PageTitle(UserTalkAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode), revision.user, viewModel.pageTitle.wikiSite)
                requestTalk.launch(TalkTemplatesActivity.newIntent(requireContext(), pageTitle, fromRevisionId = viewModel.revisionFromId, toRevisionId = viewModel.revisionToId))
            }
        }

        binding.errorView.backClickListener = View.OnClickListener { requireActivity().finish() }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_edit_details, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        menu.findItem(R.id.menu_view_edit_history).isVisible = viewModel.fromRecentEdits
        menu.findItem(R.id.menu_report_feature).isVisible = viewModel.fromRecentEdits
        menu.findItem(R.id.menu_learn_more).isVisible = viewModel.fromRecentEdits
        menu.findItem(R.id.menu_saved_messages).isVisible = viewModel.fromRecentEdits
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share_edit -> {
                sendPatrollerExperienceEvent("top_menu_share_click", "pt_edit")
                ShareUtil.shareText(requireContext(), StringUtil.fromHtml(viewModel.pageTitle.displayText).toString(), getSharableDiffUrl())
                editHistoryInteractionEvent?.logShareClick()
                true
            }
            R.id.menu_copy_link_to_clipboard -> {
                sendPatrollerExperienceEvent("top_menu_copy_click", "pt_edit")
                copyLink(getSharableDiffUrl())
                true
            }
            R.id.menu_view_edit_history -> {
                sendPatrollerExperienceEvent("top_menu_history_click", "pt_edit")
                startActivity(EditHistoryListActivity.newIntent(requireContext(), viewModel.pageTitle))
                true
            }
            R.id.menu_learn_more -> {
                sendPatrollerExperienceEvent("top_menu_learn_click", "pt_edit")
                FeedbackUtil.showAndroidAppEditingFAQ(requireContext())
                true
            }
            R.id.menu_report_feature -> {
                sendPatrollerExperienceEvent("top_menu_feedback_click", "pt_edit")
                showFeedbackOptionsDialog(true)
                true
            }
            R.id.menu_saved_messages -> {
                sendPatrollerExperienceEvent("diff_saved_init", "pt_warning_messages")
                val pageTitle = PageTitle(UserTalkAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode), viewModel.pageTitle.text, viewModel.pageTitle.wikiSite)
                requireActivity().startActivity(TalkTemplatesActivity.newIntent(requireContext(), pageTitle, true))
                true
            }
            else -> false
        }
    }

    private fun showUserPopupMenu(revision: Revision?, showThankButton: Boolean, anchorView: View) {
        revision?.let {
            UserTalkPopupHelper.show(requireActivity() as AppCompatActivity,
                PageTitle(UserAliasData.valueFor(viewModel.pageTitle.wikiSite.languageCode),
                    it.user, viewModel.pageTitle.wikiSite), it.isAnon, anchorView,
                InvokeSource.DIFF_ACTIVITY, HistoryEntry.SOURCE_EDIT_DIFF_DETAILS,
                revisionId = if (showThankButton) it.revId else null, pageId = viewModel.pageId, showUserInfo = true)
        }
    }

    private fun maybeShowOneTimeSequentialRecentEditsTooltips() {
        if (Prefs.showOneTimeSequentialRecentEditsDiffTooltip && viewModel.fromRecentEdits &&
            binding.oresDamagingButton.isVisible && binding.oresGoodFaithButton.isVisible &&
            parentFragment == FragmentUtil.getAncestor(this, SuggestedEditsCardsFragment::class.java)?.topBaseChild()) {
            Prefs.showOneTimeSequentialRecentEditsDiffTooltip = false
            binding.root.removeCallbacks(sequentialTooltipRunnable)
            binding.root.postDelayed(sequentialTooltipRunnable, 500)
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
        binding.navTabContainer.isVisible = false
        binding.diffRecyclerView.isVisible = false
        binding.diffUnavailableContainer.isVisible = false
        binding.thankButton.isVisible = false
        binding.undoButton.isVisible = false
        binding.olderIdButton.isVisible = false
        binding.newerIdButton.isVisible = false
        binding.warnButton.isVisible = viewModel.fromRecentEdits
    }

    private fun updateAfterRevisionFetchSuccess() {
        binding.articleTitleView.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)

        if (viewModel.revisionFrom != null) {
            binding.usernameFromButton.text = viewModel.revisionFrom!!.user
            binding.revisionFromTimestamp.text = DateUtil.getTimeAndDateString(requireContext(), viewModel.revisionFrom!!.timeStamp)
            binding.revisionFromEditComment.text = StringUtil.fromHtml(viewModel.revisionFrom!!.parsedcomment.trim())
            binding.revisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color))
            binding.overlayRevisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.progressive_color))
            binding.usernameFromButton.isVisible = true
            binding.revisionFromEditComment.isVisible = true
        } else {
            binding.usernameFromButton.isVisible = false
            binding.revisionFromEditComment.isVisible = false
            binding.revisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.placeholder_color))
            binding.overlayRevisionFromTimestamp.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.placeholder_color))
            binding.revisionFromTimestamp.text = getString(R.string.revision_initial_none)
        }
        binding.overlayRevisionFromTimestamp.text = binding.revisionFromTimestamp.text

        binding.oresDamagingButton.isVisible = false
        binding.oresGoodFaithButton.isVisible = false

        viewModel.revisionTo?.let {
            binding.usernameToButton.text = it.user
            binding.revisionToTimestamp.text = DateUtil.getTimeAndDateString(requireContext(), it.timeStamp)
            binding.overlayRevisionToTimestamp.text = binding.revisionToTimestamp.text
            binding.revisionToEditComment.text = StringUtil.fromHtml(it.parsedcomment.trim())

            if (it.ores != null && viewModel.fromRecentEdits) {
                binding.oresDamagingButton.isVisible = true
                binding.oresDamagingButton.text = getString(R.string.edit_damage, ((it.ores?.damagingProb ?: 0f) * 100f).toInt().toString().plus("%"))
                binding.oresDamagingButton.setOnClickListener(openQualityAndIntentFiltersPage)
                binding.oresGoodFaithButton.isVisible = true
                binding.oresGoodFaithButton.text = getString(R.string.edit_intent, ((it.ores?.goodfaithProb ?: 0f) * 100f).toInt().toString().plus("%"))
                binding.oresGoodFaithButton.setOnClickListener(openQualityAndIntentFiltersPage)

                maybeShowOneTimeSequentialRecentEditsTooltips()
            }
        }

        binding.olderIdButton.isVisible = true
        binding.newerIdButton.isVisible = true
        setEnableDisableTint(binding.newerIdButton, !viewModel.canGoForward)
        setEnableDisableTint(binding.olderIdButton, viewModel.revisionFromId == 0L)
        binding.newerIdButton.isEnabled = viewModel.canGoForward
        binding.olderIdButton.isEnabled = viewModel.revisionFromId != 0L

        binding.thankIcon.setImageResource(R.drawable.ic_heart_outline_24)

        binding.revisionDetailsView.isVisible = true
        binding.errorView.isVisible = false
    }

    private val openQualityAndIntentFiltersPage = View.OnClickListener { view ->
        if (view.id == R.id.oresDamagingButton) {
            sendPatrollerExperienceEvent("quality_click", "pt_edit")
        } else {
            sendPatrollerExperienceEvent("intent_click", "pt_edit")
        }
        UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.quality_and_intent_filters_url)))
    }

    private fun updateAfterDiffFetchSuccess() {
        binding.diffRecyclerView.isVisible = true
    }

    private fun setEnableDisableTint(view: ImageView, isDisabled: Boolean) {
        ImageViewCompat.setImageTintList(view, AppCompatResources.getColorStateList(requireContext(),
            ResourceUtil.getThemedAttributeId(requireContext(), if (isDisabled)
                R.attr.inactive_color else R.attr.secondary_color)))
    }

    private fun updateWatchButton(hasWatchlistExpiry: Boolean) {
        binding.watchButton.isVisible = AccountUtil.isLoggedIn
        binding.watchLabel.text = getString(if (viewModel.isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
        binding.watchIcon.setImageResource(
            if (viewModel.isWatched && !hasWatchlistExpiry) {
                R.drawable.ic_star_24
            } else if (!viewModel.isWatched) {
                R.drawable.ic_baseline_star_outline_24
            } else {
                R.drawable.ic_baseline_star_half_24
            }
        )
    }

    private fun showWatchlistSnackbar() {
        updateWatchButton(false)
        if (!viewModel.isWatched) {
            sendPatrollerExperienceEvent("unwatch_success_toast", "pt_watchlist")
            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.watchlist_page_removed_from_watchlist_snackbar, viewModel.pageTitle.displayText))
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, @DismissEvent event: Int) {
                        if (!isAdded) {
                            return
                        }
                        showFeedbackOptionsDialog()
                    }
                })
                .setAnchorView(binding.navTabContainer)
                .show()
        } else if (viewModel.isWatched) {
            sendPatrollerExperienceEvent("watch_success_toast", "pt_watchlist")
            FeedbackUtil.makeSnackbar(requireActivity(),
                    getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                            viewModel.pageTitle.displayText,
                            getString(WatchlistExpiry.NEVER.stringId)))
                .setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                    ExclusiveBottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(viewModel.pageTitle, WatchlistExpiry.NEVER))
                }
                        .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar, @DismissEvent event: Int) {
                        if (!isAdded) {
                            return
                        }
                        showFeedbackOptionsDialog()
                    }
                })
                .setAnchorView(binding.navTabContainer)
                .show()
        }
    }

    private fun showThankSnackbar() {
        binding.thankIcon.setImageResource(R.drawable.ic_heart_24)
        binding.thankButton.isEnabled = false
        FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.thank_success_message, viewModel.revisionTo?.user))
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, @DismissEvent event: Int) {
                    if (!isAdded) {
                        return
                    }
                    showFeedbackOptionsDialog()
                }
            })
            .setAnchorView(binding.navTabContainer)
            .show()
        sendPatrollerExperienceEvent("thank_success", "pt_thank")
    }

    private fun showThankDialog() {
        val parent = FrameLayout(requireContext())
        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setView(parent)
                .setPositiveButton(R.string.thank_dialog_positive_button_text) { _, _ ->
                    sendPatrollerExperienceEvent("thank_confirm", "pt_thank")
                    viewModel.sendThanks(viewModel.pageTitle.wikiSite, viewModel.revisionToId)
                }
                .setNegativeButton(R.string.thank_dialog_negative_button_text) { _, _ ->
                    sendPatrollerExperienceEvent("thank_cancel", "pt_thank")
                    editHistoryInteractionEvent?.logThankCancel()
                }
                .create()
        dialog.layoutInflater.inflate(R.layout.view_thank_dialog, parent)
        dialog.show()
    }

    private fun showUndoDialog() {
        val dialog = UndoEditDialog(editHistoryInteractionEvent, requireActivity(),
            if (viewModel.fromRecentEdits) InvokeSource.SUGGESTED_EDITS_RECENT_EDITS else null) { text ->
                viewModel.revisionTo?.let {
                    binding.progressBar.isVisible = true
                    EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                    viewModel.undoEdit(viewModel.pageTitle, it.user, text.toString(), viewModel.revisionToId, 0)
                }
        }
        dialog.show()
    }

    private fun showUndoSnackbar() {
        FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.patroller_tasks_patrol_edit_undo_success))
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, @DismissEvent event: Int) {
                    if (!isAdded) {
                        return
                    }
                    showFeedbackOptionsDialog()
                }
            })
            .setAnchorView(binding.navTabContainer)
            .show()
    }

    private fun showRollbackDialog() {
        MaterialAlertDialogBuilder(requireActivity())
            .setMessage(R.string.revision_rollback_dialog_title)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                sendPatrollerExperienceEvent("rollback_confirm", "pt_edit")
                binding.progressBar.isVisible = true
                viewModel.revisionTo?.let {
                    EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle, EditAttemptStepEvent.INTERFACE_OTHER)
                    viewModel.postRollback(viewModel.pageTitle, it.user)
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                sendPatrollerExperienceEvent("rollback_cancel", "pt_edit")
            }
            .show()
    }

    private fun showRollbackSnackbar() {
        FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.patroller_tasks_patrol_edit_rollback_success))
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar, @DismissEvent event: Int) {
                    if (!isAdded) {
                        return
                    }
                    showFeedbackOptionsDialog()
                }
            })
            .setAnchorView(binding.navTabContainer)
            .show()
    }

    private fun showFeedbackOptionsDialog(skipPreference: Boolean = false) {
        if (!viewModel.fromRecentEdits || (!skipPreference && !Prefs.showOneTimeRecentEditsFeedbackForm)) {
            return
        }
        if (Prefs.showOneTimeRecentEditsFeedbackForm) {
            sendPatrollerExperienceEvent("toolbar_first_feedback", "pt_feedback")
        }
        SurveyDialog.showFeedbackOptionsDialog(requireActivity(), InvokeSource.SUGGESTED_EDITS_RECENT_EDITS)
    }

    private fun updateActionButtons() {
        binding.undoButton.isVisible = viewModel.revisionFrom != null && AccountUtil.isLoggedIn
        binding.thankButton.isEnabled = true
        binding.thankButton.isVisible = AccountUtil.isLoggedIn &&
                !AccountUtil.userName.equals(viewModel.revisionTo?.user) &&
                viewModel.revisionTo?.isAnon == false
    }

    private fun getSharableDiffUrl(): String {
        return viewModel.pageTitle.getWebApiUrl("diff=${viewModel.revisionToId}&oldid=${viewModel.revisionFromId}&variant=${viewModel.pageTitle.wikiSite.languageCode}")
    }

    override fun onExpiryChanged(expiry: WatchlistExpiry) {
        sendPatrollerExperienceEvent("expiry_" + expiry.expiry.replace(" ", "_"), "pt_watchlist")
        updateWatchButton(expiry != WatchlistExpiry.NEVER)
        showFeedbackOptionsDialog()
    }

    private fun copyLink(uri: String?) {
        ClipboardUtil.setPlainText(requireContext(), text = uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    private fun sendPatrollerExperienceEvent(action: String, activeInterface: String, actionData: String = "") {
        if (viewModel.fromRecentEdits) {
            PatrollerExperienceEvent.logAction(action, activeInterface, actionData)
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(title: PageTitle, pageId: Int, revisionFrom: Long, revisionTo: Long, source: InvokeSource): ArticleEditDetailsFragment {
            return ArticleEditDetailsFragment().apply {
                arguments = bundleOf(ArticleEditDetailsActivity.EXTRA_ARTICLE_TITLE to title,
                    ArticleEditDetailsActivity.EXTRA_PAGE_ID to pageId,
                    ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_FROM to revisionFrom,
                    ArticleEditDetailsActivity.EXTRA_EDIT_REVISION_TO to revisionTo,
                    Constants.INTENT_EXTRA_INVOKE_SOURCE to source)
            }
        }
    }
}
