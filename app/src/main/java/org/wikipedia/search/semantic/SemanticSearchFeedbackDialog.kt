package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wikimedia.testkitchen.instrument.Funnel
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.extensions.getString
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.search.SearchResult
import org.wikipedia.util.FeedbackUtil

class SemanticSearchFeedbackDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    private val viewModel: SemanticSearchFeedbackViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    SemanticSearchFeedbackScreen(
                        languageCode = viewModel.languageCode,
                        selectedRating = viewModel.selectedRating.collectAsState().value,
                        onRatingClick = viewModel::selectRating,
                        onCloseClick = {
                            SemanticSearchAbTest().shouldSendEvent {
                                viewModel.instrument.submitInteraction("click",
                                    actionSubtype = "article_result",
                                    elementId = "feedback_close_button"
                                )
                            }
                            dismiss()
                        },
                        onSubmitClick = { isPositive, feedbackText ->
                            SemanticSearchAbTest().shouldSendEvent {
                                isPositive?.let {
                                    viewModel.instrument.submitInteraction("click",
                                        actionSubtype = "article_result",
                                        elementId = "feedback_submit",
                                        actionContext = mapOf(
                                            "score" to if (it) "1" else "0",
                                            "text" to feedbackText,
                                            "search_id_sem" to viewModel.searchIdSemantic,
                                            "type" to SearchResult.SearchResultType.SEMANTIC
                                        )
                                    )
                                }
                            }
                            viewModel.submitFeedback()
                            val message = requireContext().getString(viewModel.languageCode, R.string.semantic_search_results_feedback_submitted_message)
                            FeedbackUtil.showMessage(requireActivity(), message)
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedbackSubmitted.first { it == true }
            SemanticSearchAbTest().shouldSendEvent {
                viewModel.instrument.submitInteraction("error",
                    actionSubtype = "article_result",
                    elementId = "feedback_prompt",
                    actionContext = mapOf(
                        "error_type" to FeedbackSuppression.DISMISSED.value
                    )
                )
            }
        }
    }

    companion object {
        const val ARG_LANGUAGE_CODE = "languageCode"
        const val ARG_SEARCH_ID_SEMANTIC = "searchIdSemantic"
        const val ARG_FUNNEL = "funnel"
        const val ARTICLE_DISPLAY_DELAY_MILLIS = 5000L

        fun newInstance(languageCode: String, searchIdSemantic: String?, funnel: Funnel?): SemanticSearchFeedbackDialog {
            val dialog = SemanticSearchFeedbackDialog()
            dialog.arguments = Bundle().apply {
                putString(ARG_LANGUAGE_CODE, languageCode)
                putString(ARG_SEARCH_ID_SEMANTIC, searchIdSemantic)
                putSerializable(ARG_FUNNEL, funnel)
            }
            return dialog
        }
    }

    enum class FeedbackSuppression(val value: String) {
        LEFT_ARTICLE("left_article"), // The reader left the article before the card was due.
        STARTED_ACTIVITY("started_activity"), // The reader started something else in the article: find in page, share, and so on.
        SCREEN_COVERED("screen_covered"), // Another screen was on top when the card was due.
        DISMISSED("dismissed") // The card was dismissed by the reader.
    }
}
