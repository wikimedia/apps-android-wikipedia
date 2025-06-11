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
            val activeInterface = if (viewModel.fromSettings) "settings_hub_select" else "rrl_hub_select"

            RecommendedReadingListEvent.submit("impression", activeInterface, optionsShown = viewModel.availableSources.map { it.eventString }.toString())

            setContent {
                BaseTheme {
                    SourceSelectionScreen(
                        uiState = viewModel.uiSourceState.collectAsState().value,
                        fromSettings = viewModel.fromSettings,
                        onCloseClick = {
                            RecommendedReadingListEvent.submit("close_click", activeInterface)
                            if (viewModel.fromSettings) {
                                viewModel.saveSourceSelection().let { (_, selectedSource) ->
                                    RecommendedReadingListEvent.submit(
                                        action = "submit_click",
                                        activeInterface = activeInterface,
                                        optionsShown = viewModel.availableSources.map { it.eventString }.toString(),
                                        selected = selectedSource.eventString,
                                        currentSetting = Prefs.recommendedReadingListSource.eventString
                                    )
                                }
                                requireActivity().setResult(RESULT_OK)
                            }
                            requireActivity().finish()
                        },
                        onSourceClick = {
                            RecommendedReadingListEvent.submit("${it.eventString}_click", activeInterface)
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
                                        RecommendedReadingListEvent.submit("built_cancel_click", activeInterface)
                                    }
                                    .setNegativeButton(R.string.recommended_reading_list_settings_updates_base_dialog_positive_button) { _, _ ->
                                        RecommendedReadingListEvent.submit("adjust_click", activeInterface)
                                        viewModel.updateSourceSelection(newSource = it)

                                        if (it == RecommendedReadingListSource.INTERESTS) {
                                            viewModel.saveSourceSelection()
                                            requireActivity().supportFragmentManager.beginTransaction()
                                                .add(android.R.id.content, RecommendedReadingListInterestsFragment.newInstance(viewModel.fromSettings))
                                                .addToBackStack(null).commit()
                                        }
                                    }
                                    .show()
                            } else {
                                viewModel.updateSourceSelection(newSource = it)
                            }
                        },
                        onNextClick = {
                            viewModel.saveSourceSelection().let { (shouldGoToInterests, selectedSource) ->

                                RecommendedReadingListEvent.submit(
                                    action = "submit_click",
                                    activeInterface = activeInterface,
                                    optionsShown = viewModel.availableSources.map { it.eventString }.toString(),
                                    selected = selectedSource.eventString,
                                    currentSetting = Prefs.recommendedReadingListSource.eventString
                                )

                                if (shouldGoToInterests) {
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .add(android.R.id.content, RecommendedReadingListInterestsFragment.newInstance())
                                        .addToBackStack(null).commit()
                                } else {
                                    Prefs.isRecommendedReadingListEnabled = true
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
