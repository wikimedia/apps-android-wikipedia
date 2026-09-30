package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
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
                            dismiss()
                        },
                        onSubmitClick = { isPositive, feedbackText ->
                            // TODO: send the feedback to instrumentation in another ticket.
                            FeedbackUtil.showMessage(requireActivity(), R.string.semantic_search_results_feedback_submitted_message)
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_LANGUAGE_CODE = "languageCode"
        const val ARTICLE_DISPLAY_DELAY_MILLIS = 5000L

        fun newInstance(languageCode: String): SemanticSearchFeedbackDialog {
            val dialog = SemanticSearchFeedbackDialog()
            dialog.arguments = Bundle().apply {
                putString(ARG_LANGUAGE_CODE, languageCode)
            }
            return dialog
        }
    }
}
