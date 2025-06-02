package org.wikipedia.readinglist.recommended

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
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
                        fromSettings = viewModel.fromSettings,
                        onCloseClick = {
                            if (viewModel.fromSettings) {
                                viewModel.saveSourceSelection()
                                requireActivity().setResult(RESULT_OK)
                            }
                            requireActivity().finish()
                        },
                        onSourceClick = {
                            when (it) {
                                RecommendedReadingListSource.INTERESTS -> {
                                    viewModel.updateSourceSelection(newSource = RecommendedReadingListSource.INTERESTS)
                                }
                                RecommendedReadingListSource.READING_LIST -> {
                                    viewModel.updateSourceSelection(newSource = RecommendedReadingListSource.READING_LIST)
                                }
                                RecommendedReadingListSource.HISTORY -> {
                                    viewModel.updateSourceSelection(newSource = RecommendedReadingListSource.HISTORY)
                                }
                            }
                        },
                        onNextClick = {
                            viewModel.saveSourceSelection().let { shouldGoToInterests ->
                                if (shouldGoToInterests) {
                                    // TODO: Navigate to interests screen
                                } else {
                                    // TODO: Navigate to Discover screen
                                }
                            }
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
        fun newInstance(fromSettings: Boolean = false): RecommendedReadingListSourceFragment {
            return RecommendedReadingListSourceFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(Constants.ARG_BOOLEAN, fromSettings)
                }
            }
        }
    }
}
