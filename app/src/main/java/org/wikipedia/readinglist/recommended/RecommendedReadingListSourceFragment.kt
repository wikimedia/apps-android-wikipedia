package org.wikipedia.readinglist.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme

class RecommendedReadingListSourceFragment : Fragment() {

    private val viewModel: RecommendedReadingListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    SourceSelectionScreen(
                        uiState = viewModel.uiSourceState.collectAsState().value,
                        onCloseClick = {
                            requireActivity().finish()
                        },
                        onInterestsClick = {
                            viewModel.updateSourceSelection(newSource = RecommendedReadingListSource.INTERESTS)
                        },
                        onSavedClick = {
                            viewModel.updateSourceSelection(newSource = RecommendedReadingListSource.READING_LIST)
                        },
                        onHistoryClick = {
                            viewModel.updateSourceSelection(newSource = RecommendedReadingListSource.HISTORY)
                        },
                        onNextClick = {
                            // viewModel.handleNextAction()
                        },
                        wikiErrorClickEvents = WikiErrorClickEvents(
                            backClickListener = {
                                requireActivity().finish()
                            },
                            retryClickListener = {
                                viewModel.setupSourceSelection()
                            }
                        )
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
