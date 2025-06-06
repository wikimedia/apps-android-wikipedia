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
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode

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
                            RecommendedReadingListEvent.submit("close_click", "rrl_hub_select")
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
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .add(android.R.id.content, RecommendedReadingListInterestsFragment.newInstance())
                                        .addToBackStack(null).commit()
                                } else {
                                    startActivity(ReadingListActivity.newIntent(requireContext(), readingListMode = ReadingListMode.RECOMMENDED))
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
                    putBoolean(RecommendedReadingListOnboardingActivity.EXTRA_FROM_SETTINGS, fromSettings)
                }
            }
        }
    }
}
