package org.wikipedia.search

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.LongPressHandler
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.eventplatform.PlacesEvent
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
import org.wikipedia.views.ViewUtil.loadImageWithRoundedCorners

class SearchResultsFragment : Fragment() {
    interface Callback {
        fun onSearchAddPageToList(entry: HistoryEntry, addToDefault: Boolean)
        fun onSearchMovePageToList(sourceReadingListId: Long, entry: HistoryEntry)
        fun onSearchProgressBar(enabled: Boolean)
        fun navigateToTitle(item: PageTitle, inNewTab: Boolean, position: Int, location: Location? = null)
        fun setSearchText(text: CharSequence)
    }

    private var _binding: FragmentSearchResultsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchResultsViewModel by viewModels()
    private val searchResultsAdapter = SearchResultsAdapter()
    private val noSearchResultAdapter = NoSearchResultAdapter()
    private val searchResultsConcatAdapter = ConcatAdapter(searchResultsAdapter)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        binding.searchResultsList.layoutManager = LinearLayoutManager(requireActivity())
        binding.searchResultsList.adapter = searchResultsConcatAdapter
        binding.searchErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.searchErrorView.retryClickListener = View.OnClickListener {
            binding.searchErrorView.visibility = View.GONE
            startSearch(viewModel.searchTerm, true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.searchResultsFlow.collectLatest {
                        binding.searchResultsList.visibility = View.VISIBLE
                        searchResultsAdapter.submitData(it)
                    }
                }
                launch {
                    searchResultsAdapter.loadStateFlow.collectLatest {
                        callback()?.onSearchProgressBar(it.append is LoadState.Loading || it.refresh is LoadState.Loading)
                        val showEmpty = (it.append is LoadState.NotLoading && it.append.endOfPaginationReached && searchResultsAdapter.itemCount == 0)
                        if (showEmpty) {
                            searchResultsConcatAdapter.addAdapter(noSearchResultAdapter)
                        } else {
                            searchResultsConcatAdapter.removeAdapter(noSearchResultAdapter)
                        }
                    }
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        binding.searchErrorView.retryClickListener = null
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
        if (!force && viewModel.searchTerm == term && viewModel.languageCode == searchLanguageCode) {
            return
        }

        viewModel.searchTerm = term
        viewModel.languageCode = searchLanguageCode

        if (term.isNullOrBlank()) {
            clearResults()
            return
        }

        binding.searchResultsList.scrollToPosition(0)
        searchResultsAdapter.refresh()
    }

    private fun clearResults() {
        binding.searchResultsList.visibility = View.GONE
        binding.searchErrorView.visibility = View.GONE
        binding.searchErrorView.visibility = View.GONE
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
            return false
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

    private inner class NoSearchResultAdapter : RecyclerView.Adapter<NoSearchResultItemViewHolder>() {
        override fun onBindViewHolder(holder: NoSearchResultItemViewHolder, position: Int) {
            holder.bindItem(position, viewModel.resultsCount[position])
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoSearchResultItemViewHolder {
            return NoSearchResultItemViewHolder(ItemSearchNoResultsBinding.inflate(layoutInflater, parent, false))
        }

        override fun getItemCount(): Int { return viewModel.resultsCount.size }
    }

    private inner class NoSearchResultItemViewHolder(val itemBinding: ItemSearchNoResultsBinding) : DefaultViewHolder<View>(itemBinding.root) {
        private val accentColorStateList = getThemedColorStateList(requireContext(), R.attr.progressive_color)
        private val secondaryColorStateList = getThemedColorStateList(requireContext(), R.attr.secondary_color)
        fun bindItem(position: Int, resultsCount: Int) {
            if (resultsCount == 0 && viewModel.invokeSource == Constants.InvokeSource.PLACES) {
                PlacesEvent.logAction("no_results_impression", "search_view")
            }
            val langCode = WikipediaApp.instance.languageState.appLanguageCodes[position]
            itemBinding.resultsText.text = if (resultsCount == 0) getString(R.string.search_results_count_zero) else resources.getQuantityString(R.plurals.search_results_count, resultsCount, resultsCount)
            itemBinding.resultsText.setTextColor(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            itemBinding.languageCode.visibility = if (viewModel.resultsCount.size == 1) View.GONE else View.VISIBLE
            itemBinding.languageCode.setLangCode(langCode)
            itemBinding.languageCode.setTextColor(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            itemBinding.languageCode.setBackgroundTint(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
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

            if (type === SearchResult.SearchResultType.SEARCH) {
                itemBinding.pageListIcon.visibility = View.GONE
            } else {
                itemBinding.pageListIcon.visibility = View.VISIBLE
                itemBinding.pageListIcon.setImageResource(if (type === SearchResult.SearchResultType.HISTORY) R.drawable.ic_history_24 else if (type === SearchResult.SearchResultType.TAB_LIST) R.drawable.ic_tab_one_24px else R.drawable.ic_bookmark_white_24dp)
            }
            // highlight search term within the text
            StringUtil.boldenKeywordText(itemBinding.pageListItemTitle, pageTitle.displayText, viewModel.searchTerm)
            itemBinding.pageListItemImage.visibility = if (pageTitle.thumbUrl.isNullOrEmpty()) if (type === SearchResult.SearchResultType.SEARCH) View.GONE else View.INVISIBLE else View.VISIBLE
            loadImageWithRoundedCorners(itemBinding.pageListItemImage, pageTitle.thumbUrl)

            view.isLongClickable = true
            view.setOnClickListener {
                callback()?.navigateToTitle(searchResult.pageTitle, false, position, searchResult.location)
            }
            view.setOnCreateContextMenuListener(LongPressHandler(view,
                    HistoryEntry.SOURCE_SEARCH, SearchResultsFragmentLongPressHandler(position), pageTitle))
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    fun setInvokeSource(invokeSource: Constants.InvokeSource) {
        viewModel.invokeSource = invokeSource
    }

    private val searchLanguageCode get() =
        if (isAdded) (requireParentFragment() as SearchFragment).searchLanguageCode else WikipediaApp.instance.languageState.appLanguageCode
}
