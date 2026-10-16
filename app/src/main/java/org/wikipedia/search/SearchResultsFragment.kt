package org.wikipedia.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.extensions.instrument
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.search.semantic.SemanticSearchAbTest
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.util.FeedbackUtil

class SearchResultsFragment : Fragment() {

    private var composeView: ComposeView? = null
    private val viewModel: SearchResultsViewModel by viewModels()
    private val semanticSearchAbTest = SemanticSearchAbTest()
    val isShowing get() = composeView?.visibility == View.VISIBLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.searchTermForLogging.collectLatest { query ->
                    if (!query.isNullOrEmpty()) {
                        requireActivity().instrument?.submitInteraction(
                            "search_init", actionContext = mapOf("query" to query)
                        )
                        semanticSearchAbTest.shouldSendEvent {
                            val isFromRecentSearch = viewModel.isFromRecentSearch
                            requireActivity().instrument?.submitInteraction("init",
                                actionSubtype = if (isFromRecentSearch) "recent_search" else null,
                                elementId = if (isFromRecentSearch) "search_result" else null,
                                actionContext = mapOf(
                                    "query" to query,
                                    "query_trunc" to (query.length > 256)
                                )
                            )
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.lexicalResultsForLogging.collectLatest { data ->
                    if (data != null) {
                        requireActivity().instrument?.submitInteraction(
                            "show_search_result",
                            actionContext = viewModel.getStandardEventActionContext()
                        )
                        submitSemanticLexicalResultEvent("impression")
                    }
                }
            }
        }

        return ComposeView(requireActivity()).apply {
            composeView = this
            setContent {
                BaseTheme {
                    SearchResultsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToTitle = { result, inNewTab, position, location ->

                            requireActivity().instrument?.submitInteraction("search_result_click",
                                pageData = TestKitchenAdapter.getPageData(result.pageTitle),
                                actionContext = viewModel.getStandardEventActionContext(result)
                            )

                            submitSemanticLexicalResultEvent("click", result)

                            callback()?.navigateToTitle(result.pageTitle, inNewTab, position, location)
                        },
                        onItemLongClick = { view, searchResult, position ->
                            val entry =
                                HistoryEntry(searchResult.pageTitle, HistoryEntry.SOURCE_SEARCH)
                            LongPressMenu(
                                view,
                                callback = SearchResultLongPressHandler(callback(), position)
                            ).show(entry)
                        },
                        onCloseSearch = { requireActivity().finish() },
                        onRetrySearch = {
                            viewModel.refreshSearchResults()
                        },
                        onLanguageClick = { position ->
                            if (isAdded && position >= 0) {
                                (requireParentFragment() as SearchFragment).setUpLanguageScroll(
                                    position
                                )
                            }
                        },
                        onLoading = { enabled ->
                            callback()?.onSearchProgressBar(enabled)
                        },
                        onSemanticSearchClick = { searchTerm, isFirstUse ->
                            semanticSearchAbTest.shouldSendEvent {
                                requireActivity().instrument?.submitInteraction("click",
                                    elementId = "enter_semantic_search"
                                )
                                if (isFirstUse) {
                                    requireActivity().instrument?.submitInteraction("click",
                                        elementId = "try_now_button"
                                    )
                                }
                            }
                            searchTerm?.let {
                                callback()?.showSemanticSearchResultsDialog(it)
                            }
                        },
                        onSemanticSearchInfoClick = {
                            semanticSearchAbTest.shouldSendEvent {
                                requireActivity().instrument?.submitInteraction("click",
                                    elementId = "info_button"
                                )
                            }
                            callback()?.showSemanticSearchInfoDialog()
                        },
                        onSemanticSearchCloseClick = {
                            semanticSearchAbTest.shouldSendEvent {
                                requireActivity().instrument?.submitInteraction("click",
                                    elementId = "close_semantic_button"
                                )
                            }
                            val messageText = requireContext().getString(searchLanguageCode, R.string.semantic_search_snackbar_message)
                            val actionButtonText = requireContext().getString(searchLanguageCode, R.string.semantic_search_snackbar_action_label)
                            FeedbackUtil.makeSnackbar(requireActivity(), messageText)
                                .setAction(actionButtonText) {
                                    startActivity(SettingsActivity.newIntent(requireContext()))
                                }
                                .show()
                        }
                    )
                }
            }
        }
    }

    fun show() {
        composeView?.visibility = View.VISIBLE
    }

    fun hide() {
        composeView?.visibility = View.GONE
    }

    fun startSearch(term: String?, force: Boolean, isFromRecentSearch: Boolean) {
        if (!force && viewModel.searchTerm.value == term && viewModel.languageCode.value == searchLanguageCode) {
            return
        }

        requireActivity().instrument?.setDefaultMediaWikiData(WikiSite.forLanguageCode(searchLanguageCode).dbName())
        viewModel.updateLanguageCode(searchLanguageCode)
        viewModel.updateSearchTerm(if (term.isNullOrBlank()) "" else term)
        viewModel.isFromRecentSearch = isFromRecentSearch

        // If user changes the language, make sure to turn off hybrid search screen.
        if (force) {
            viewModel.refreshSearchResults()
        }
    }

    private fun callback(): SearchResultCallback? {
        return getCallback(this, SearchResultCallback::class.java)
    }

    private fun submitSemanticLexicalResultEvent(action: String, result: SearchResult? = null) {
        semanticSearchAbTest.shouldSendEvent {
            val isTreatment = semanticSearchAbTest.group == ABTest.GROUP_2
            requireActivity().instrument?.submitInteraction(action,
                elementId = "search_result",
                actionSubtype = if (isTreatment) "lexical_result" else null,
                actionContext = if (isTreatment) viewModel.getSemanticEventActionContext(result) else viewModel.getStandardEventActionContext(result)
            )
        }
    }

    fun setInvokeSource(invokeSource: Constants.InvokeSource) {
        viewModel.invokeSource = invokeSource
    }

    fun setSemanticSearchId(searchId: String) {
        viewModel.lastXSearchIdSemantic = searchId
    }

    val isSemanticSearchFeedbackShown get() = viewModel.isSemanticSearchFeedbackShown

    fun onSemanticSearchFeedbackShown() {
        viewModel.isSemanticSearchFeedbackShown = true
    }

    private val searchLanguageCode
        get() =
            if (isAdded) (requireParentFragment() as SearchFragment).searchLanguageCode else WikipediaApp.instance.languageState.appLanguageCode

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }
}
