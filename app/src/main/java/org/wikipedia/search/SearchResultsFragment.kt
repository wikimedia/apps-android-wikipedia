package org.wikipedia.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class SearchResultsFragment : Fragment() {

    private var composeView: ComposeView? = null
    private val viewModel: SearchResultsViewModel by viewModels()
    var showHybridSearch by mutableStateOf(false)

    val isShowing get() = composeView?.visibility == View.VISIBLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            composeView = this
            setContent {
                BaseTheme {
                    if (showHybridSearch && viewModel.isHybridSearchExperimentOn) {
                        HybridSearchResultsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToTitle = { title, inNewTab, position, location ->
                                callback()?.navigateToTitle(title, inNewTab, position, location)
                            },
                            onSemanticItemClick = { title, inNewTab, position, location ->
                                // TODO: update the callback to navigate to the specific section
                                callback()?.navigateToTitle(title, inNewTab, position, location)
                            },
                            onItemLongClick = { view, searchResult, position ->
                                val entry = HistoryEntry(searchResult.pageTitle, HistoryEntry.SOURCE_SEARCH)
                                LongPressMenu(view, callback = SearchResultLongPressHandler(callback(), position)).show(entry)
                            },
                            onInfoClick = {
                                UriUtil.visitInExternalBrowser(requireActivity(), getString(R.string.hybrid_search_info_link).toUri())
                            },
                            onCloseSearch = { requireActivity().finish() },
                            onRetrySearch = {
                                viewModel.refreshSearchResults()
                            },
                            onLoading = { enabled ->
                                callback()?.onSearchProgressBar(enabled)
                            },
                            onRatingClick = { isPositive, isToggled ->
                                // TODO: implement rating submission
                            }
                        )
                    } else {
                        SearchResultsScreen(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            onNavigateToTitle = { title, inNewTab, position, location ->
                                callback()?.navigateToTitle(title, inNewTab, position, location)
                            },
                            onItemLongClick = { view, searchResult, position ->
                                val entry =
                                    HistoryEntry(searchResult.pageTitle, HistoryEntry.SOURCE_SEARCH)
                                LongPressMenu(
                                    view,
                                    callback = SearchResultLongPressHandler(callback(), position)
                                ).show(entry)
                            },
                            onSemanticSearchClick = {
                                callback()?.setSearchText(StringUtil.fromHtml(it))
                                showHybridSearch = true
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
                            }
                        )
                    }
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

    fun startSearch(term: String?, force: Boolean, resetHybridSearch: Boolean = false) {
        if (!force && viewModel.searchTerm.value == term && viewModel.languageCode.value == searchLanguageCode) {
            return
        }

        if (force) {
            viewModel.refreshSearchResults()
        } else {
            viewModel.updateSearchTerm(if (term.isNullOrBlank()) "" else term)
            viewModel.updateLanguageCode(searchLanguageCode)
        }

        // If user changes the language, make sure to turn off hybrid search screen.
        showHybridSearch = !resetHybridSearch && showHybridSearch && viewModel.isHybridSearchExperimentOn

        if (showHybridSearch) {
            viewModel.loadHybridSearchResults()
        }
    }

    private fun callback(): SearchResultCallback? {
        return getCallback(this, SearchResultCallback::class.java)
    }

    fun setInvokeSource(invokeSource: Constants.InvokeSource) {
        viewModel.invokeSource = invokeSource
    }

    private val searchLanguageCode
        get() =
            if (isAdded) (requireParentFragment() as SearchFragment).searchLanguageCode else WikipediaApp.instance.languageState.appLanguageCode

    override fun onDestroyView() {
        super.onDestroyView()
        composeView = null
    }
}
