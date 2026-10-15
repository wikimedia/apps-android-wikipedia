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
import org.wikipedia.extensions.getString
import org.wikipedia.extensions.instrument
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.search.SearchFragment
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResultCallback
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil

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
                        onItemClick = { searchResult ->

                            SemanticSearchAbTest().shouldSendEvent {
                                requireActivity().instrument?.submitInteraction("click",
                                    actionSubtype = "semantic_result",
                                    elementId = "search_result",
                                    actionContext = mapOf(
                                        "position" to searchResult.indexInApiCall,
                                        "search_id_sem" to viewModel.lastXSearchIdSemantic,
                                        "type" to searchResult.type
                                    )
                                )
                            }

                            callback()?.navigateToTitle(
                                item = searchResult.pageTitle,
                                inNewTab = false,
                                position = searchResult.indexInApiCall,
                                showSemanticSearchFeedback = viewModel.deferFeedbackToArticle(),
                                highlightText = StringUtil.extractStringFromStyle("searchmatch", searchResult.snippet)
                            )
                        },
                        onCloseClick = {
                            SemanticSearchAbTest().shouldSendEvent {
                                val isFeedbackVisible = viewModel.feedbackState.value.isVisible
                                requireActivity().instrument?.submitInteraction("click",
                                    actionSubtype = if (isFeedbackVisible) "cardview_result" else "semantic_result",
                                    elementId = if (isFeedbackVisible) "feedback_close_button" else "close_result_button"
                                )
                            }
                            dismiss()
                        },
                        onFeedbackSubmit = { isPositive, feedbackText ->
                            SemanticSearchAbTest().shouldSendEvent {
                                isPositive?.let {
                                    requireActivity().instrument?.submitInteraction("click",
                                        actionSubtype = "cardview_result",
                                        elementId = "feedback_submit",
                                        actionContext = mapOf(
                                            "score" to if (it) "1" else "0",
                                            "text" to feedbackText,
                                            "search_id_sem" to viewModel.lastXSearchIdSemantic,
                                            "type" to SearchResult.SearchResultType.SEMANTIC
                                        )
                                    )
                                }
                            }
                            val message = requireContext().getString(viewModel.languageCode, R.string.semantic_search_results_feedback_submitted_message)
                            FeedbackUtil.makeSnackbar(requireView(), message).show()
                        },
                        onLoading = {
                            if (it != LoadingType.Loading) {
                                SemanticSearchAbTest().shouldSendEvent {
                                    requireActivity().instrument?.submitInteraction("impression",
                                        actionSubtype = if (it != LoadingType.Empty) "semantic_result" else "semantic_result",
                                        elementId = "search_result",
                                        actionContext = if (it != LoadingType.Empty) {
                                            mapOf(
                                                "search_id_sem" to viewModel.lastXSearchIdSemantic
                                            )
                                        } else null
                                    )
                                }

                                if (it == LoadingType.Success) {
                                    (parentFragment as SearchFragment).setSemanticSearchId(
                                        viewModel.lastXSearchIdSemantic
                                    )
                                }
                            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedbackState.first {
                it.isVisible && it.isPositive == null
            }
            SemanticSearchAbTest().shouldSendEvent {
                requireActivity().instrument?.submitInteraction("error",
                    actionSubtype = "cardview_result",
                    elementId = "feedback_prompt",
                    actionContext = mapOf(
                        "error_type" to SemanticSearchFeedbackDialog.FeedbackSuppression.DISMISSED.value
                    )
                )
            }
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
