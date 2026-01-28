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
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.LongPressMenu

class SearchResultsFragment : Fragment() {

    private var composeView: ComposeView? = null
    private val viewModel: SearchResultsViewModel by viewModels()

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
                    SearchResultsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize(),
                        onNavigateToTitle = { title, inNewTab, position, location ->
                            callback()?.navigateToTitle(title, inNewTab, position, location)
                        },
                        onItemLongClick = { view, searchResult, position ->
                            val entry = HistoryEntry(searchResult.pageTitle, HistoryEntry.SOURCE_SEARCH)
                            LongPressMenu(view, callback = SearchResultLongPressHandler(callback(), position)).show(entry)
                        },
                        onCloseSearch = { requireActivity().finish() },
                        onRetrySearch = {
                            viewModel.refreshSearchResults()
                        },
                        onLanguageClick = { position ->
                            if (isAdded && position >= 0) {
                                (requireParentFragment() as SearchFragment).setUpLanguageScroll(position)
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

    fun show() {
        composeView?.visibility = View.VISIBLE
    }

    fun hide() {
        composeView?.visibility = View.GONE
    }

    fun startSearch(term: String?, force: Boolean) {
        if (!force && viewModel.searchTerm.value == term && viewModel.languageCode.value == searchLanguageCode) {
            return
        }

        if (force) {
            viewModel.refreshSearchResults()
        } else {
            viewModel.updateSearchTerm(if (term.isNullOrBlank()) "" else term)
            viewModel.updateLanguageCode(searchLanguageCode)
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
