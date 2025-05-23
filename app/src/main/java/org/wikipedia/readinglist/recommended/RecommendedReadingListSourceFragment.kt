package org.wikipedia.readinglist.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.compose.theme.BaseTheme

class RecommendedReadingListSourceFragment : Fragment() {

    private val viewModel: RecommendedReadingListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState = viewModel.uiSourceState.collectAsState().value
                // TODO: add loading and error
                BaseTheme {
                    SourceSelectionScreen(
                        onCloseClick = {
                            requireActivity().finish()
                        },
                        onInterestsClick = {
                            // Handle interests option click, e.g., navigate to interests screen
                            // viewModel.handleInterestsSelection()
                        },
                        onSavedClick = {
                            // Handle saved option click, e.g., navigate to saved articles
                            // viewModel.handleSavedSelection()
                        },
                        onHistoryClick = {
                            // Handle history option click, e.g., navigate to history
                            // viewModel.handleHistorySelection()
                        },
                        onNextClick = {
                            // Handle next button click
                            // viewModel.handleNextAction()
                        }
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance(): RecommendedReadingListSourceFragment {
            return RecommendedReadingListSourceFragment()
        }
    }
}
