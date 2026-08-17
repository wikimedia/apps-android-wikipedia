package org.wikipedia.readinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.compose.SaveArticleSheetContent
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil

class SaveArticleSheetDialog : ExtendedBottomSheetDialogFragment() {

    private val viewModel: SaveArticleSheetViewModel by viewModels()
    private val collapsedSheetPeekHeightPx
        get() = minOf(
            DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.saveArticleSheetPeekHeight)),
            (DimenUtil.displayHeightPx * MAX_PEEK_HEIGHT_RATIO).toInt()
        )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    SaveArticleSheetContent(
                        article = uiState.article,
                        collections = uiState.collections,
                        collapsedSheetPeekHeightPx = collapsedSheetPeekHeightPx.toFloat(),
                        onArticleHeaderClick = viewModel::toggleArticleSaved,
                        onCreateCollectionClick = viewModel::requestNewCollection,
                        onCollectionRowClick = viewModel::toggleArticleInCollection
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).apply {
            peekHeight = collapsedSheetPeekHeightPx
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleEvent)
            }
        }
    }

    private fun handleEvent(event: SaveArticleSheetEvent) {
        when (event) {
            is SaveArticleSheetEvent.ShowCreateCollectionDialog -> {
                showCreateCollectionDialog(event.existingTitles)
            }
            is SaveArticleSheetEvent.ArticleAddedToCollection -> {
                val activity = requireActivity()
                FeedbackUtil.makeSnackbar(
                    activity,
                    activity.getString(
                        R.string.reading_list_article_added_to_named,
                        StringUtil.fromHtml(viewModel.pageTitle.displayText),
                        event.list.title
                    )
                ).setAction(R.string.reading_list_added_view_button) {
                    activity.startActivity(ReadingListActivity.newIntent(activity, event.list))
                }.show()
                dismiss()
            }
            is SaveArticleSheetEvent.ArticleRemovedFromCollection -> {
                val activity = requireActivity()
                FeedbackUtil.makeSnackbar(
                    activity,
                    activity.getString(
                        R.string.reading_list_item_deleted_from_list,
                        StringUtil.fromHtml(viewModel.pageTitle.displayText),
                        event.list.title
                    )
                ).show()
                dismiss()
            }
            SaveArticleSheetEvent.ArticleRemovedFromAllCollections -> {
                val activity = requireActivity()
                FeedbackUtil.makeSnackbar(
                    activity,
                    resources.getQuantityString(
                        R.plurals.reading_lists_articles_removed_from_collections,
                        1,
                        1
                    )
                ).show()
                dismiss()
            }
            is SaveArticleSheetEvent.CollectionArticleLimitReached -> {
                FeedbackUtil.makeSnackbar(
                    requireActivity(),
                    getString(
                        R.string.reading_list_article_limit_message,
                        event.list.title,
                        Constants.MAX_READING_LIST_ARTICLE_LIMIT
                    )
                ).show()
            }
            is SaveArticleSheetEvent.ShowUnsaveConfirmation -> {
                showUnsaveArticleConfirmationDialog(event.lists)
            }
            SaveArticleSheetEvent.Dismiss -> {
                dismiss()
            }
            SaveArticleSheetEvent.ListLimitReached -> {
                val activity = requireActivity()
                dismiss()
                FeedbackUtil.makeSnackbar(
                    activity,
                    activity.getString(R.string.reading_lists_limit_message)
                ).show()
            }
        }
    }

    private fun showUnsaveArticleConfirmationDialog(lists: List<ReadingList>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reading_lists_remove_articles_confirm_dialog_title)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.reading_lists_unsave_articles_confirm_dialog_message,
                    1,
                    1
                )
            )
            .setPositiveButton(R.string.reading_lists_remove_articles_confirm_button) { _, _ ->
                viewModel.confirmUnsaveArticle(lists)
            }
            .setNegativeButton(R.string.reading_list_delete_dialog_cancel_button_text, null)
            .show()
    }

    private fun showCreateCollectionDialog(existingTitles: List<String>) {
        ReadingListTitleDialog.readingListTitleDialog(
            activity = requireActivity(),
            title = "",
            description = "",
            otherTitles = existingTitles,
            callback = object : ReadingListTitleDialog.Callback {
                override fun onSuccess(text: String, description: String) {
                    viewModel.createCollection(text, description)
                }
            }
        ).show()
    }

    companion object {
        private const val MAX_PEEK_HEIGHT_RATIO = 0.75f

        fun show(fragmentManager: FragmentManager, pageTitle: PageTitle) {
            ExclusiveBottomSheetPresenter.show(fragmentManager, newInstance(pageTitle))
        }

        fun show(activity: FragmentActivity, pageTitle: PageTitle) {
            show(activity.supportFragmentManager, pageTitle)
        }

        private fun newInstance(pageTitle: PageTitle): SaveArticleSheetDialog {
            return SaveArticleSheetDialog().apply {
                arguments = bundleOf(Constants.ARG_TITLE to pageTitle)
            }
        }
    }
}
