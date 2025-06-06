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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode
import org.wikipedia.settings.Prefs

class RecommendedReadingListSourceFragment : Fragment() {

    private val viewModel: RecommendedReadingListSourceViewModel by viewModels()

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
                            if (viewModel.fromSettings) {
                                if (it == Prefs.recommendedReadingListSource) {
                                    viewModel.updateSourceSelection(newSource = it)
                                    return@SourceSelectionScreen
                                }
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.recommended_reading_list_settings_updates_base_dialog_title)
                                    .setMessage(R.string.recommended_reading_list_settings_updates_base_dialog_message)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.recommended_reading_list_settings_updates_base_dialog_negative_button) { _, _ ->
                                        RecommendedReadingListEvent.submit("built_cancel_click", "discover_settings")
                                    }
                                    .setNegativeButton(R.string.recommended_reading_list_settings_updates_base_dialog_positive_button) { _, _ ->
                                        RecommendedReadingListEvent.submit("adjust_click", "discover_settings")
                                        viewModel.updateSourceSelection(newSource = it)
                                    }
                                    .show()
                            } else {
                                viewModel.updateSourceSelection(newSource = it)
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
                                    requireActivity().finish()
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
