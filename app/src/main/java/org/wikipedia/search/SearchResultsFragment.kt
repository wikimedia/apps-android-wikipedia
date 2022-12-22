package org.wikipedia.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.LongPressHandler
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.SearchFunnel
import org.wikipedia.databinding.FragmentSearchResultsBinding
import org.wikipedia.databinding.ItemSearchNoResultsBinding
import org.wikipedia.databinding.ItemSearchResultBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil.getThemedColorStateList
import org.wikipedia.util.StringUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.ViewUtil.formatLangButton
import org.wikipedia.views.ViewUtil.loadImageWithRoundedCorners

class SearchResultsFragment : Fragment() {
    interface Callback {
        fun onSearchAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
        fun onSearchMovePageToList(sourceReadingListId: Long, entry: HistoryEntry)
        fun onSearchProgressBar(enabled: Boolean)
        fun navigateToTitle(item: PageTitle, inNewTab: Boolean, position: Int)
        fun setSearchText(text: CharSequence)
        fun getFunnel(): SearchFunnel
    }

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchResultsViewModel by viewModels { SearchResultsViewModel.Factory(callback()?.getFunnel()) }
    private val searchResultsAdapter = SearchResultsAdapter()
    private var currentSearchTerm: String? = ""
    private var lastFullTextResults: SearchResults? = null
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        binding.searchResultsList.layoutManager = LinearLayoutManager(requireActivity())
        binding.searchResultsList.adapter = searchResultsAdapter
        binding.searchErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.searchErrorView.retryClickListener = View.OnClickListener {
            binding.searchErrorView.visibility = View.GONE
            startSearch(currentSearchTerm, true)
        }

        lifecycleScope.launch {
            viewModel.searchResultsFlow.collectLatest {
                binding.searchResultsList.visibility = View.VISIBLE
                callback()?.onSearchProgressBar(false)
                searchResultsAdapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenCreated {
            searchResultsAdapter.loadStateFlow.collectLatest {
                val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && searchResultsAdapter.itemCount == 0)
                if (showEmpty) {
                    // TODO: show search count adapter
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        binding.searchErrorView.retryClickListener = null
        disposables.clear()
        _binding = null
        super.onDestroyView()
    }

    fun show() {
        binding.searchResultsDisplay.visibility = View.VISIBLE
    }

    fun hide() {
        binding.searchResultsDisplay.visibility = View.GONE
    }

    val isShowing get() = binding.searchResultsDisplay.visibility == View.VISIBLE

    fun setLayoutDirection(langCode: String) {
        setConditionalLayoutDirection(binding.searchResultsList, langCode)
    }

    fun startSearch(term: String?, force: Boolean) {
        if (!force && currentSearchTerm == term) {
            return
        }
        callback()?.onSearchProgressBar(true)
        currentSearchTerm = term
        if (term.isNullOrBlank()) {
            clearResults()
            return
        }
        viewModel.searchTerm = term
        viewModel.languageCode = searchLanguageCode
        searchResultsAdapter.refresh()
    }

    private fun clearResults() {
        binding.searchResultsList.visibility = View.GONE
        binding.searchErrorView.visibility = View.GONE
        binding.searchErrorView.visibility = View.GONE
        lastFullTextResults = null
        viewModel.resultsCount.clear()
        binding.searchResultsList.adapter?.notifyDataSetChanged()
    }

    private inner class SearchResultsFragmentLongPressHandler(private val lastPositionRequested: Int) : LongPressMenu.Callback {
        override fun onOpenLink(entry: HistoryEntry) {
            callback()?.navigateToTitle(entry.title, false, lastPositionRequested)
        }

        override fun onOpenInNewTab(entry: HistoryEntry) {
            callback()?.navigateToTitle(entry.title, true, lastPositionRequested)
        }

        override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
            callback()?.onSearchAddPageToList(entry, addToDefault)
        }

        override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
            page.let {
                callback()?.onSearchMovePageToList(page!!.listId, entry)
            }
        }
    }

    private inner class SearchResultsDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return oldItem.pageTitle.prefixedText == newItem.pageTitle.prefixedText &&
                        oldItem.pageTitle.namespace == newItem.pageTitle.namespace
        }

        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    private inner class SearchResultsAdapter : PagingDataAdapter<SearchResult, DefaultViewHolder<View>>(SearchResultsDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<View> {
            return SearchResultItemViewHolder(ItemSearchResultBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<View>, pos: Int) {
            getItem(pos)?.let {
                (holder as SearchResultItemViewHolder).bindItem(pos, it)
            }
        }
    }

    private inner class NoSearchResultItemViewHolder(val itemBinding: ItemSearchNoResultsBinding) : DefaultViewHolder<View>(itemBinding.root) {
        private val accentColorStateList = getThemedColorStateList(requireContext(), R.attr.colorAccent)
        private val secondaryColorStateList = getThemedColorStateList(requireContext(), R.attr.material_theme_secondary_color)
        fun bindItem(position: Int) {
            val langCode = WikipediaApp.instance.languageState.appLanguageCodes[position]
            val resultsCount = viewModel.resultsCount[position]
            itemBinding.resultsText.text = if (resultsCount == 0) getString(R.string.search_results_count_zero) else resources.getQuantityString(R.plurals.search_results_count, resultsCount, resultsCount)
            itemBinding.resultsText.setTextColor(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            itemBinding.languageCode.visibility = if (viewModel.resultsCount.size == 1) View.GONE else View.VISIBLE
            itemBinding.languageCode.text = langCode
            itemBinding.languageCode.setTextColor(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            ViewCompat.setBackgroundTintList(itemBinding.languageCode, if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            formatLangButton(itemBinding.languageCode, langCode,
                    SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
            view.isEnabled = resultsCount > 0
            view.setOnClickListener {
                if (!isAdded) {
                    return@setOnClickListener
                }
                (requireParentFragment() as SearchFragment).setUpLanguageScroll(position)
            }
        }
    }

    private inner class SearchResultItemViewHolder(val itemBinding: ItemSearchResultBinding) : DefaultViewHolder<View>(itemBinding.root) {
        fun bindItem(position: Int, searchResult: SearchResult) {
            val (pageTitle, redirectFrom, type) = searchResult
            if (redirectFrom.isNullOrEmpty()) {
                itemBinding.pageListItemRedirect.visibility = View.GONE
                itemBinding.pageListItemRedirectArrow.visibility = View.GONE
                itemBinding.pageListItemDescription.text = pageTitle.description
            } else {
                itemBinding.pageListItemRedirect.visibility = View.VISIBLE
                itemBinding.pageListItemRedirectArrow.visibility = View.VISIBLE
                itemBinding.pageListItemRedirect.text = getString(R.string.search_redirect_from, redirectFrom)
                itemBinding.pageListItemDescription.visibility = View.GONE
            }

            itemBinding.pageListIcon.visibility = View.VISIBLE
            itemBinding.pageListIcon.setImageResource(if (type === SearchResult.SearchResultType.HISTORY) R.drawable.ic_history_24 else if (type === SearchResult.SearchResultType.TAB_LIST) R.drawable.ic_tab_one_24px else R.drawable.ic_bookmark_white_24dp)

            // highlight search term within the text
            StringUtil.boldenKeywordText(itemBinding.pageListItemTitle, pageTitle.displayText, currentSearchTerm)
            itemBinding.pageListItemImage.visibility = if (pageTitle.thumbUrl.isNullOrEmpty()) if (type === SearchResult.SearchResultType.SEARCH) View.GONE else View.INVISIBLE else View.VISIBLE
            loadImageWithRoundedCorners(itemBinding.pageListItemImage, pageTitle.thumbUrl)

            view.isLongClickable = true
            view.setOnClickListener {
                callback()?.navigateToTitle(searchResult.pageTitle, false, position)
            }
            view.setOnCreateContextMenuListener(LongPressHandler(view,
                    HistoryEntry.SOURCE_SEARCH, SearchResultsFragmentLongPressHandler(position), pageTitle))
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    private val searchLanguageCode get() =
        if (isAdded) (requireParentFragment() as SearchFragment).searchLanguageCode else WikipediaApp.instance.languageState.appLanguageCode
}
