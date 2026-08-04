package org.wikipedia.readinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.compose.SaveArticleSheetContent
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil

class SaveArticleSheetDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    private val viewModel: SaveArticleSheetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    SaveArticleSheetContent(
                        article = uiState.article,
                        collections = uiState.collections,
                        onArticleHeaderClick = viewModel::toggleArticleSaved,
                        onCreateCollectionClick = viewModel::requestNewCollection,
                        onCollectionRowClick = viewModel::toggleArticleInCollection
                    )
                }
            }
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
        fun newInstance(pageTitle: PageTitle): SaveArticleSheetDialog {
            return SaveArticleSheetDialog().apply {
                arguments = bundleOf(Constants.ARG_TITLE to pageTitle)
            }
        }
    }
}
