package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.search.SearchResultCallback
import org.wikipedia.util.FeedbackUtil

class SemanticSearchResultsDialog : ExtendedBottomSheetDialogFragment() {

    private val viewModel: SemanticSearchResultsViewModel by viewModels()

    override fun getTheme() = R.style.App_BottomSheetDialogTheme_BelowStatusBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    SemanticSearchResultsScreen(
                        viewModel = viewModel,
                        onItemClick = { title ->
                            callback()?.navigateToTitle(title, false, 0)
                        },
                        onCloseClick = {
                            dismiss()
                        },
                        onFeedbackSubmit = { isPositive, feedbackText ->
                            // TODO: send the feedback to instrumentation in another ticket.
                            FeedbackUtil.makeSnackbar(requireView(), getString(R.string.semantic_search_results_feedback_submitted_message)).show()
                        },
                        onLoading = {
                            // TODO: maybe instrumentation?
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedbackState.first { it.isVisible }
            callback()?.onSemanticSearchFeedbackShown()
        }
    }

    private fun callback(): SearchResultCallback? {
        return getCallback(this, SearchResultCallback::class.java)
    }

    companion object {
        const val ARG_SEARCH_QUERY = "searchQuery"
        const val ARG_LANGUAGE_CODE = "languageCode"
        const val ARG_SHOW_FEEDBACK = "showFeedback"

        fun newInstance(query: String, languageCode: String, invokeSource: Constants.InvokeSource, showFeedback: Boolean): SemanticSearchResultsDialog {
            val dialog = SemanticSearchResultsDialog()
            dialog.arguments = Bundle().apply {
                putString(ARG_SEARCH_QUERY, query)
                putString(ARG_LANGUAGE_CODE, languageCode)
                putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
                putBoolean(ARG_SHOW_FEEDBACK, showFeedback)
            }
            return dialog
        }
    }
}
