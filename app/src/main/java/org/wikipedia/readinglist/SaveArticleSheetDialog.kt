package org.wikipedia.readinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.wikipedia.Constants
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.compose.SaveArticleSheetContent

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
                        onArticleSaveClick = viewModel::onArticleSaveClick,
                        onCreateCollectionClick = viewModel::onCreateCollectionClick,
                        onCollectionClick = viewModel::onCollectionClick
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(pageTitle: PageTitle): SaveArticleSheetDialog {
            return SaveArticleSheetDialog().apply {
                arguments = bundleOf(Constants.ARG_TITLE to pageTitle)
            }
        }
    }
}
