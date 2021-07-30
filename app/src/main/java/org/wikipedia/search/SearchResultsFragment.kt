package org.wikipedia.search

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.LongPressHandler
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.SearchFunnel
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentSearchResultsBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.StringUtil.boldenKeywordText
import org.wikipedia.util.StringUtil.fromHtml
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.GoneIfEmptyTextView
import org.wikipedia.views.ViewUtil.formatLangButton
import org.wikipedia.views.ViewUtil.loadImageWithRoundedCorners
import java.util.*
import java.util.concurrent.TimeUnit

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
    private val searchResultsCache = LruCache<String, MutableList<SearchResult>>(MAX_CACHE_SIZE_SEARCH_RESULTS)
    private val searchResultsCountCache = LruCache<String, List<Int>>(MAX_CACHE_SIZE_SEARCH_RESULTS)
    private var currentSearchTerm: String? = ""
    private var lastFullTextResults: SearchResults? = null
    private val totalResults = mutableListOf<SearchResult>()
    private val resultsCountList = mutableListOf<Int>()
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchResultsBinding.inflate(inflater, container, false)
        binding.searchResultsList.layoutManager = LinearLayoutManager(requireActivity())
        binding.searchResultsList.adapter = SearchResultAdapter()
        binding.searchErrorView.backClickListener = View.OnClickListener { requireActivity().finish() }
        binding.searchErrorView.retryClickListener = View.OnClickListener {
            binding.searchErrorView.visibility = View.GONE
            startSearch(currentSearchTerm, true)
        }
        binding.searchSuggestion.setOnClickListener { onSuggestionClick() }
        return binding.root
    }

    override fun onDestroyView() {
        binding.searchErrorView.retryClickListener = null
        disposables.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun onSuggestionClick() {
        val suggestion = binding.searchSuggestion.tag as String
        callback()?.getFunnel()?.searchDidYouMean(searchLanguageCode)
        callback()?.setSearchText(suggestion)
        startSearch(suggestion, true)
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
        cancelSearchTask()
        currentSearchTerm = term
        if (term.isNullOrBlank()) {
            clearResults()
            return
        }
        val cacheResult = searchResultsCache["$searchLanguageCode-$term"]
        val cacheResultsCount = searchResultsCountCache["$searchLanguageCode-$term"]
        if (!cacheResult.isNullOrEmpty()) {
            clearResults()
            displayResults(cacheResult)
            return
        } else if (!cacheResultsCount.isNullOrEmpty()) {
            clearResults()
            displayResultsCount(cacheResultsCount)
            return
        }
        doTitlePrefixSearch(term, force)
    }

    fun clearSearchResultsCountCache() {
        searchResultsCountCache.evictAll()
    }

    private fun doTitlePrefixSearch(searchTerm: String, force: Boolean) {
        cancelSearchTask()
        val startTime = System.nanoTime()
        updateProgressBar(true)
        disposables.add(Observable.timer(if (force) 0 else DELAY_MILLIS.toLong(), TimeUnit.MILLISECONDS).flatMap {
            Observable.zip(ServiceFactory.get(WikiSite.forLanguageCode(searchLanguageCode)).prefixSearch(searchTerm, BATCH_SIZE, searchTerm),
                    if (searchTerm.length >= 2) Observable.fromCallable { AppDatabase.instance.readingListPageDao().findPageForSearchQueryInAnyList(searchTerm) } else Observable.just(SearchResults()),
                    if (searchTerm.length >= 2) Observable.fromCallable { AppDatabase.instance.historyEntryWithImageDao().findHistoryItem(searchTerm) } else Observable.just(SearchResults()),
                    { searchResponse, readingListSearchResults, historySearchResults ->

                        val searchResults = searchResponse?.query?.pages()?.let {
                            SearchResults(it, WikiSite.forLanguageCode(searchLanguageCode),
                                searchResponse.continuation,
                                searchResponse.suggestion())
                        } ?: SearchResults()

                        handleSuggestion(searchResults.suggestion)
                        val resultList = mutableListOf<SearchResult>()
                        addSearchResultsFromTabs(resultList)
                        resultList.addAll(readingListSearchResults!!.results)
                        resultList.addAll(historySearchResults.results)
                        resultList.addAll(searchResults.results)
                        resultList
                    })
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate { updateProgressBar(false) }
                .subscribe({ results ->
                    binding.searchErrorView.visibility = View.GONE
                    handleResults(results, searchTerm, startTime)
                }) { caught ->
                    binding.searchErrorView.visibility = View.VISIBLE
                    binding.searchErrorView.setError(caught)
                    binding.searchResultsContainer.visibility = View.GONE
                    logError(false, startTime)
                })
    }

    private fun addSearchResultsFromTabs(resultList: MutableList<SearchResult>) {
        currentSearchTerm?.let { term ->
            if (term.length < 2) {
                return
            }
            WikipediaApp.getInstance().tabList.forEach { tab ->
                tab.backStackPositionTitle?.let {
                    if (it.displayText.lowercase(Locale.getDefault()).contains(term.lowercase(Locale.getDefault()))) {
                        resultList.add(SearchResult(it, SearchResult.SearchResultType.TAB_LIST))
                        return
                    }
                }
            }
        }
    }

    private fun handleResults(resultList: MutableList<SearchResult>, searchTerm: String, startTime: Long) {
        // To ease data analysis and better make the funnel track with user behaviour,
        // only transmit search results events if there are a nonzero number of results
        if (resultList.isNotEmpty()) {
            clearResults()
            displayResults(resultList)
            log(resultList, startTime)
        }

        // add titles to cache...
        searchResultsCache.put("$searchLanguageCode-$searchTerm", resultList)

        // scroll to top, but post it to the message queue, because it should be done
        // after the data set is updated.
        binding.searchResultsList.post {
            if (!isAdded) {
                return@post
            }
            binding.searchResultsList.scrollToPosition(0)
        }
        if (resultList.isEmpty()) {
            // kick off full text search if we get no results
            doFullTextSearch(currentSearchTerm, null, true)
        }
    }

    private fun handleSuggestion(suggestion: String?) {
        if (suggestion != null) {
            binding.searchSuggestion.text = fromHtml("<u>" +
                    getString(R.string.search_did_you_mean, suggestion) + "</u>")
            binding.searchSuggestion.tag = suggestion
            binding.searchSuggestion.visibility = View.VISIBLE
        } else {
            binding.searchSuggestion.visibility = View.GONE
        }
    }

    private fun cancelSearchTask() {
        updateProgressBar(false)
        disposables.clear()
    }

    private fun doFullTextSearch(searchTerm: String?,
                                 continueOffset: Map<String, String>?,
                                 clearOnSuccess: Boolean) {
        val startTime = System.nanoTime()
        updateProgressBar(true)
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(searchLanguageCode)).fullTextSearch(searchTerm, BATCH_SIZE,
                continueOffset?.get("continue"), continueOffset?.get("gsroffset"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response ->
                    response.query?.pages()?.let {
                        // noinspection ConstantConditions
                        return@map SearchResults(it, WikiSite.forLanguageCode(searchLanguageCode), response.continuation, null)
                    }
                    SearchResults()
                }
                .flatMap { results ->
                    val resultList = results.results
                    cache(resultList, searchTerm!!)
                    log(resultList, startTime)
                    if (clearOnSuccess) {
                        clearResults(false)
                    }
                    binding.searchErrorView.visibility = View.GONE

                    // full text special:
                    lastFullTextResults = results
                    if (resultList.isNotEmpty()) {
                        displayResults(resultList)
                    } else {
                        updateProgressBar(true)
                    }
                    if (resultList.isEmpty()) doSearchResultsCountObservable(searchTerm) else Observable.empty()
                }
                .toList()
                .doAfterTerminate { updateProgressBar(false) }
                .subscribe({ list ->
                    var resultsCount = list
                    if (resultsCount.isNotEmpty()) {

                        // make a singleton list if all results are empty.
                        var sum = 0
                        for (count in resultsCount) {
                            sum += count
                            if (sum > 0) {
                                break
                            }
                        }
                        if (sum == 0) {
                            resultsCount = listOf(0)
                        }
                        searchResultsCountCache.put("$searchLanguageCode-$searchTerm", resultsCount)
                        displayResultsCount(resultsCount)
                    }
                }) {
                    // If there's an error, just log it and let the existing prefix search results be.
                    logError(true, startTime)
                })
    }

    private fun doSearchResultsCountObservable(searchTerm: String?): Observable<Int> {
        return Observable.fromIterable(WikipediaApp.getInstance().language().appLanguageCodes)
                .concatMap { langCode ->
                    if (langCode == searchLanguageCode) {
                        return@concatMap Observable.just(MwQueryResponse())
                    }
                    ServiceFactory.get(WikiSite.forLanguageCode(langCode)).prefixSearch(searchTerm, BATCH_SIZE, searchTerm)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .flatMap { response ->
                                response.query?.pages()?.let {
                                    return@flatMap Observable.just(response)
                                }
                                ServiceFactory.get(WikiSite.forLanguageCode(langCode)).fullTextSearch(searchTerm, BATCH_SIZE, null, null)
                            }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { response -> response.query?.pages()?.size ?: 0 }
    }

    private fun updateProgressBar(enabled: Boolean) {
        callback()?.onSearchProgressBar(enabled)
    }

    private fun clearResults(clearSuggestion: Boolean = true) {
        binding.searchResultsContainer.visibility = View.GONE
        binding.searchErrorView.visibility = View.GONE
        binding.searchResultsContainer.visibility = View.GONE
        binding.searchErrorView.visibility = View.GONE
        if (clearSuggestion) {
            binding.searchSuggestion.visibility = View.GONE
        }
        lastFullTextResults = null
        totalResults.clear()
        resultsCountList.clear()
        adapter.notifyDataSetChanged()
    }

    private val adapter: SearchResultAdapter
        get() = binding.searchResultsList.adapter as SearchResultAdapter

    private fun displayResults(results: List<SearchResult>) {
        for (newResult in results) {
            var contains = false
            for ((pageTitle) in totalResults) {
                if (newResult.pageTitle == pageTitle) {
                    contains = true
                    break
                }
            }
            if (!contains) {
                totalResults.add(newResult)
            }
        }
        binding.searchResultsContainer.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
    }

    private fun displayResultsCount(list: List<Int>) {
        resultsCountList.clear()
        resultsCountList.addAll(list)
        binding.searchResultsContainer.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
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

    private inner class SearchResultAdapter : RecyclerView.Adapter<DefaultViewHolder<View>>() {
        override fun getItemViewType(position: Int): Int {
            return if (totalResults.isEmpty()) VIEW_TYPE_NO_RESULTS else VIEW_TYPE_ITEM
        }

        override fun getItemCount(): Int {
            return if (totalResults.isEmpty()) resultsCountList.size else totalResults.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefaultViewHolder<View> {
            return if (viewType == VIEW_TYPE_ITEM) {
                SearchResultItemViewHolder(LayoutInflater.from(context)
                        .inflate(R.layout.item_search_result, parent, false))
            } else {
                NoSearchResultItemViewHolder(LayoutInflater.from(context)
                        .inflate(R.layout.item_search_no_results, parent, false))
            }
        }

        override fun onBindViewHolder(holder: DefaultViewHolder<View>, pos: Int) {
            if (holder is SearchResultItemViewHolder) {
                holder.bindItem(pos)
            } else if (holder is NoSearchResultItemViewHolder) {
                holder.bindItem(pos)
            }
        }
    }

    private inner class NoSearchResultItemViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        private val accentColorStateList = ColorStateList.valueOf(getThemedColor(requireContext(), R.attr.colorAccent))
        private val secondaryColorStateList = ColorStateList.valueOf(getThemedColor(requireContext(), R.attr.material_theme_secondary_color))
        fun bindItem(position: Int) {
            val langCode = WikipediaApp.getInstance().language().appLanguageCodes[position]
            val resultsCount = resultsCountList[position]
            val resultsText = view.findViewById<TextView>(R.id.results_text)
            val languageCodeText = view.findViewById<TextView>(R.id.language_code)
            resultsText.text = if (resultsCount == 0) getString(R.string.search_results_count_zero) else resources.getQuantityString(R.plurals.search_results_count, resultsCount, resultsCount)
            resultsText.setTextColor(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            languageCodeText.visibility = if (resultsCountList.size == 1) View.GONE else View.VISIBLE
            languageCodeText.text = langCode
            languageCodeText.setTextColor(if (resultsCount == 0) secondaryColorStateList else accentColorStateList)
            languageCodeText.backgroundTintList = if (resultsCount == 0) secondaryColorStateList else accentColorStateList
            formatLangButton(languageCodeText, langCode,
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

    private inner class SearchResultItemViewHolder(itemView: View) : DefaultViewHolder<View>(itemView) {
        fun bindItem(position: Int) {
            val pageTitleText = view.findViewById<TextView>(R.id.page_list_item_title)
            val (pageTitle, redirectFrom, type) = totalResults[position]
            val searchResultItemImage = view.findViewById<ImageView>(R.id.page_list_item_image)
            val searchResultIcon = view.findViewById<ImageView>(R.id.page_list_icon)
            val descriptionText = view.findViewById<GoneIfEmptyTextView>(R.id.page_list_item_description)
            val redirectText = view.findViewById<TextView>(R.id.page_list_item_redirect)
            val redirectArrow = view.findViewById<View>(R.id.page_list_item_redirect_arrow)
            if (redirectFrom.isNullOrEmpty()) {
                redirectText.visibility = View.GONE
                redirectArrow.visibility = View.GONE
                descriptionText.text = pageTitle.description
            } else {
                redirectText.visibility = View.VISIBLE
                redirectArrow.visibility = View.VISIBLE
                redirectText.text = getString(R.string.search_redirect_from, redirectFrom)
                descriptionText.visibility = View.GONE
            }
            if (type === SearchResult.SearchResultType.SEARCH) {
                searchResultIcon.visibility = View.GONE
            } else {
                searchResultIcon.visibility = View.VISIBLE
                searchResultIcon.setImageResource(if (type === SearchResult.SearchResultType.HISTORY) R.drawable.ic_history_24 else if (type === SearchResult.SearchResultType.TAB_LIST) R.drawable.ic_tab_one_24px else R.drawable.ic_bookmark_white_24dp)
            }

            // highlight search term within the text
            boldenKeywordText(pageTitleText, pageTitle.displayText, currentSearchTerm)
            searchResultItemImage.visibility = if (pageTitle.thumbUrl.isNullOrEmpty()) if (type === SearchResult.SearchResultType.SEARCH) View.GONE else View.INVISIBLE else View.VISIBLE
            loadImageWithRoundedCorners(searchResultItemImage, pageTitle.thumbUrl)

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == totalResults.size - 1 && WikipediaApp.getInstance().isOnline) {
                if (lastFullTextResults == null) {
                    // the first full text search
                    doFullTextSearch(currentSearchTerm, null, false)
                } else if (!lastFullTextResults!!.continuation.isNullOrEmpty()) {
                    // subsequent full text searches
                    doFullTextSearch(currentSearchTerm, lastFullTextResults!!.continuation, false)
                }
            }
            view.isLongClickable = true
            view.setOnClickListener {
                if (position < totalResults.size) {
                    callback()?.navigateToTitle(totalResults[position].pageTitle, false, position)
                }
            }
            view.setOnCreateContextMenuListener(LongPressHandler(view,
                    pageTitle, HistoryEntry.SOURCE_SEARCH, SearchResultsFragmentLongPressHandler(position)))
        }
    }

    private fun cache(resultList: List<SearchResult>, searchTerm: String) {
        val cacheKey = "$searchLanguageCode-$searchTerm"
        searchResultsCache[cacheKey]?.let {
            it.addAll(resultList)
            searchResultsCache.put(cacheKey, it)
        }
    }

    private fun log(resultList: List<SearchResult>, startTime: Long) {
        // To ease data analysis and better make the funnel track with user behaviour,
        // only transmit search results events if there are a nonzero number of results
        if (resultList.isNotEmpty()) {
            // noinspection ConstantConditions
            callback()?.getFunnel()?.searchResults(true, resultList.size, displayTime(startTime), searchLanguageCode)
        }
    }

    private fun logError(fullText: Boolean, startTime: Long) {
        // noinspection ConstantConditions
        callback()?.getFunnel()?.searchError(fullText, displayTime(startTime), searchLanguageCode)
    }

    private fun displayTime(startTime: Long): Int {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime).toInt()
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    private val searchLanguageCode get() =
        if (isAdded) (requireParentFragment() as SearchFragment).searchLanguageCode else WikipediaApp.getInstance().language().appLanguageCode

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_NO_RESULTS = 1
        private const val BATCH_SIZE = 20
        private const val DELAY_MILLIS = 300
        private const val MAX_CACHE_SIZE_SEARCH_RESULTS = 4
    }
}
