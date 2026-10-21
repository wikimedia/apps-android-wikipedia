package org.wikipedia.search.semantic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import org.wikimedia.testkitchen.instrument.Funnel
import org.wikimedia.testkitchen.instrument.InstrumentImpl
import org.wikipedia.R
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.extensions.setLayoutDirectionByLang
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
                            SemanticSearchAbTest().runIfEligible {
                                viewModel.instrument.submitInteraction("click",
                                    actionSubtype = "article_result",
                                    elementId = "close_feedback_button"
                                )
                            }
                            dismiss()
                        },
                        onSubmitClick = { isPositive, feedbackText ->
                            SemanticSearchAbTest().runIfEligible {
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
                            val message = requireContext().getString(viewModel.languageCode, R.string.semantic_search_results_feedback_submitted_message)
                            FeedbackUtil.makeSnackbar(requireActivity(), message, Snackbar.LENGTH_LONG)
                                .apply { view.setLayoutDirectionByLang(viewModel.languageCode) }
                                .show()
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_LANGUAGE_CODE = "languageCode"
        const val ARG_SEARCH_ID_SEMANTIC = "searchIdSemantic"
        const val ARG_FUNNEL = "funnel"
        const val ARTICLE_DISPLAY_DELAY_MILLIS = 5000L

        fun newInstrument(funnel: Funnel?, wikiSite: WikiSite): InstrumentImpl {
            return TestKitchenAdapter.client.getInstrument("apps-search")
                .setDefaultActionSource("search")
                .setDefaultMediaWikiData(wikiSite.dbName())
                .continueFunnel(funnel)
                .also { SemanticSearchAbTest().setExperimentIfEligible(it) }
        }

        fun submitSuppressedEvent(instrument: InstrumentImpl?, actionSubtype: String, suppression: FeedbackSuppression) {
            SemanticSearchAbTest().runIfEligible {
                instrument?.submitInteraction("error",
                    actionSubtype = actionSubtype,
                    elementId = "feedback_prompt",
                    actionContext = mapOf(
                        "error_type" to suppression.value
                    )
                )
            }
        }

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
        LEFT_ARTICLE("left_article"), // The reader left the article (or closed the results) before the card was due.
        STARTED_ACTIVITY("started_activity"), // The reader was using find in page, text selection or the table of contents when the card was due.
        SCREEN_COVERED("screen_covered") // Another screen, sheet or dialog was on top (or the app was in the background) when the card was due.
    }
}
