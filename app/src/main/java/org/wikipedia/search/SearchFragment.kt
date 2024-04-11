package org.wikipedia.search

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentSearchBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.places.PlacesActivity
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.search.db.RecentSearch
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.topics.TopicsActivity
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.LanguageScrollView
import java.util.Locale

class SearchFragment : Fragment(), SearchResultsFragment.Callback, RecentSearchesFragment.Callback, LanguageScrollView.Callback {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private var app = WikipediaApp.instance
    private var tempLangCodeHolder: String? = null
    private var langBtnClicked = false
    private var isSearchActive = false
    private var query: String? = null
    private var returnLink = false
    private lateinit var recentSearchesFragment: RecentSearchesFragment
    private lateinit var searchResultsFragment: SearchResultsFragment
    private lateinit var invokeSource: InvokeSource
    private lateinit var initialLanguageList: String
    var searchLanguageCode = app.languageState.appLanguageCode
        private set

    private val searchCloseListener = SearchView.OnCloseListener {
        closeSearch()
        false
    }

    private val searchQueryListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(queryText: String): Boolean {
            DeviceUtil.hideSoftKeyboard(requireActivity())
            return true
        }

        override fun onQueryTextChange(queryText: String): Boolean {
            binding.searchCabView.setCloseButtonVisibility(queryText)
            startSearch(queryText.trim(), false)
            return true
        }
    }

    private val requestAddLanguageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            var position = 0
            val finalLanguageList = JsonUtil.encodeToString(app.languageState.appLanguageCodes)
            if (finalLanguageList != initialLanguageList) {
                requireActivity().setResult(RESULT_LANG_CHANGED)
            }
            it.data?.let { intent ->
                if (intent.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                    position = intent.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
                } else if (app.languageState.appLanguageCodes.contains(searchLanguageCode)) {
                    position = app.languageState.appLanguageCodes.indexOf(searchLanguageCode)
                }
            }
            Prefs.selectedLanguagePositionInSearch = position
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(requireActivity().intent)
        }
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        query = requireArguments().getString(ARG_QUERY)
        returnLink = requireArguments().getBoolean(SearchActivity.EXTRA_RETURN_LINK, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val childFragmentManager = childFragmentManager
        recentSearchesFragment = childFragmentManager.findFragmentById(
                R.id.search_panel_recent) as RecentSearchesFragment
        recentSearchesFragment.callback = this
        searchResultsFragment = childFragmentManager.findFragmentById(
                R.id.fragment_search_results) as SearchResultsFragment
        searchResultsFragment.setInvokeSource(invokeSource)
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.searchToolbar)
        binding.searchToolbar.setNavigationOnClickListener { requireActivity().supportFinishAfterTransition() }
        initialLanguageList = JsonUtil.encodeToString(app.languageState.appLanguageCodes).orEmpty()
        binding.searchContainer.setOnClickListener { onSearchContainerClick() }
        binding.searchLangButton.setOnClickListener { onLangButtonClick() }
        initSearchView()
        if (invokeSource == InvokeSource.PLACES) {
            PlacesEvent.logImpression("search_view")
        }

        binding.topicsButton.setOnClickListener { startActivity(Intent(requireActivity(), TopicsActivity::class.java)) }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setUpLanguageScroll(Prefs.selectedLanguagePositionInSearch)
        startSearch(query, langBtnClicked)
        binding.searchCabView.setCloseButtonVisibility(query)
        recentSearchesFragment.binding.namespacesContainer.isVisible = invokeSource != InvokeSource.PLACES
        //if (!query.isNullOrEmpty()) {
            showPanel(PANEL_SEARCH_RESULTS)
        //}
    }

    override fun onPause() {
        super.onPause()
        Prefs.selectedLanguagePositionInSearch = binding.searchLanguageScrollView.selectedPosition
    }

    override fun onResume() {
        super.onResume()

        updateTopicsButton()
        startSearch(query, true)
    }

    private fun updateTopicsButton() {
        val topics = Prefs.selectedTopics
        binding.topicsButton.text = if (topics.isEmpty())
            getString(R.string.topics_floating_button_text) else getString(R.string.topics_floating_button_text_selected, topics.size)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action && Constants.PLAIN_TEXT_MIME_TYPE == intent.type) {
            requireArguments().putString(ARG_QUERY, intent.getStringExtra(Intent.EXTRA_TEXT))
            requireArguments().putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.INTENT_SHARE)
        } else if (Intent.ACTION_PROCESS_TEXT == intent.action && Constants.PLAIN_TEXT_MIME_TYPE ==
                intent.type && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireArguments().putString(ARG_QUERY, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
            requireArguments().putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.INTENT_PROCESS_TEXT)
        }
    }

    fun setUpLanguageScroll(position: Int) {
        var pos = position
        if (app.languageState.appLanguageCodes.size > 1) {
            pos = if (app.languageState.appLanguageCodes.size > pos) pos else 0
            binding.searchLanguageScrollViewContainer.visibility = View.VISIBLE
            binding.searchLanguageScrollView.setUpLanguageScrollTabData(app.languageState.appLanguageCodes, pos, this)
            binding.searchLangButton.visibility = View.GONE
        } else {
            maybeShowMultilingualSearchTooltip()
            binding.searchLanguageScrollViewContainer.visibility = View.GONE
            binding.searchLangButton.visibility = View.VISIBLE
            initLangButton()
            recentSearchesFragment.onLangCodeChanged()
        }
    }

    private fun maybeShowMultilingualSearchTooltip() {
        if (Prefs.isMultilingualSearchTooltipShown) {
            binding.searchLangButton.postDelayed({
                if (isAdded) {
                    FeedbackUtil.showTooltip(requireActivity(), binding.searchLangButton, getString(R.string.tool_tip_lang_button),
                            aboveOrBelow = false, autoDismiss = false)
                }
            }, 500)
            Prefs.isMultilingualSearchTooltipShown = false
        }
    }

    override fun onDestroyView() {
        binding.searchCabView.setOnCloseListener(null)
        binding.searchCabView.setOnQueryTextListener(null)
        _binding = null
        super.onDestroyView()
    }

    override fun switchToSearch(text: String) {
        startSearch(text, true)
        binding.searchCabView.setQuery(text, false)
    }

    override fun onAddLanguageClicked() {
        onLangButtonClick()
    }

    override fun getLangCode(): String {
        return searchLanguageCode
    }

    override fun setSearchText(text: CharSequence) {
        binding.searchCabView.setQuery(text, false)
    }

    override fun navigateToTitle(item: PageTitle, inNewTab: Boolean, position: Int, location: Location?) {
        if (!isAdded) {
            return
        }
        if (returnLink) {
            if (invokeSource == InvokeSource.PLACES) {
                PlacesEvent.logAction("search_result_click", "search_view")
            }
            val intent = Intent().putExtra(SearchActivity.EXTRA_RETURN_LINK_TITLE, item)
                .putExtra(PlacesActivity.EXTRA_LOCATION, location)
            requireActivity().setResult(SearchActivity.RESULT_LINK_SUCCESS, intent)
            requireActivity().finish()
        } else {
            val historyEntry = HistoryEntry(item, HistoryEntry.SOURCE_SEARCH)
            startActivity(if (inNewTab) PageActivity.newIntentForNewTab(requireContext(), historyEntry, historyEntry.title)
            else PageActivity.newIntentForCurrentTab(requireContext(), historyEntry, historyEntry.title, false))
        }
        closeSearch()
    }

    override fun onSearchAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
        ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), entry.title, addToDefault, InvokeSource.SEARCH)
    }

    override fun onSearchMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
        ReadingListBehaviorsUtil.moveToList(requireActivity(), sourceReadingListId, entry.title, InvokeSource.SEARCH)
    }

    override fun onSearchProgressBar(enabled: Boolean) {
        binding.searchProgressBar.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun onSearchContainerClick() {
        // Give the root container view an empty click handler, so that click events won't
        // get passed down to any underlying views (e.g. a PageFragment on top of which
        // this fragment is shown)
    }

    private fun onLangButtonClick() {
        langBtnClicked = true
        tempLangCodeHolder = searchLanguageCode
        requestAddLanguageLauncher.launch(WikipediaLanguagesActivity.newIntent(requireActivity(), InvokeSource.SEARCH))
    }

    private fun startSearch(term: String?, force: Boolean) {
        val topics = Prefs.selectedTopics

        if (!isSearchActive) {
            openSearch()
        }
        if (topics.isEmpty() && term.isNullOrEmpty()) {
            showPanel(PANEL_RECENT_SEARCHES)
        } else if (activePanel == PANEL_RECENT_SEARCHES) {
            // start with title search...
            showPanel(PANEL_SEARCH_RESULTS)
        }

        query = term

        if (topics.isEmpty() && term.isNullOrBlank() && !force) {
            return
        }
        binding.searchContainer.postDelayed({
            if (!isAdded) {
                return@postDelayed
            }
            searchResultsFragment.startSearch(term, force)
        }, if (invokeSource == InvokeSource.PLACES || invokeSource == InvokeSource.VOICE || invokeSource == InvokeSource.INTENT_SHARE || invokeSource == InvokeSource.INTENT_PROCESS_TEXT) INTENT_DELAY_MILLIS else 0)
    }

    private fun openSearch() {
        isSearchActive = true
        binding.searchCabView.isIconified = false
        // if we already have a previous search query, then put it into the SearchView, and it will
        // automatically trigger the showing of the corresponding search results.
        if (!query.isNullOrBlank()) {
            binding.searchCabView.setQuery(query, false)
            binding.searchCabView.selectAllQueryTexts()
        }
    }

    private fun closeSearch() {
        isSearchActive = false
        DeviceUtil.hideSoftKeyboard(requireView())
        addRecentSearch(query)
    }

    private fun showPanel(panel: Int) {
        when (panel) {
            PANEL_RECENT_SEARCHES -> {
                searchResultsFragment.hide()
                recentSearchesFragment.show()
            }
            PANEL_SEARCH_RESULTS -> {
                recentSearchesFragment.hide()
                searchResultsFragment.show()
            }
        }
    }

    // otherwise, the recent searches must be showing:
    private val activePanel: Int
        get() = if (searchResultsFragment.isShowing) {
            PANEL_SEARCH_RESULTS
        } else {
            // otherwise, the recent searches must be showing:
            PANEL_RECENT_SEARCHES
        }

    private fun initSearchView() {
        binding.searchCabView.setOnQueryTextListener(searchQueryListener)
        binding.searchCabView.setOnCloseListener(searchCloseListener)
        binding.searchCabView.setSearchHintTextColor(ResourceUtil.getThemedColor(requireContext(),
                R.attr.secondary_color))
        binding.searchCabView.queryHint = getString(if (invokeSource == InvokeSource.PLACES) R.string.places_search_hint else R.string.search_hint)

        // remove focus line from search plate
        val searchEditPlate = binding.searchCabView
                .findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun initLangButton() {
        binding.searchLangButton.setLangCode(app.languageState.appLanguageCode.uppercase(Locale.ENGLISH))
        FeedbackUtil.setButtonTooltip(binding.searchLangButton)
    }

    private fun addRecentSearch(title: String?) {
        if (!title.isNullOrBlank()) {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }) {
                AppDatabase.instance.recentSearchDao().insertRecentSearch(RecentSearch(text = title))
                recentSearchesFragment.updateList()
            }
        }
    }

    override fun onLanguageTabSelected(selectedLanguageCode: String) {
        if (langBtnClicked) {
            // We need to skip an event when we return back from 'add languages' screen,
            // because it triggers two events while re-drawing the UI
            langBtnClicked = false
        } else {
            // We need a temporary language code holder because the previously selected search language code[searchLanguageCode]
            // gets overwritten when UI is re-drawn
            tempLangCodeHolder = null
        }
        searchLanguageCode = selectedLanguageCode
        searchResultsFragment.setLayoutDirection(searchLanguageCode)
        recentSearchesFragment.onLangCodeChanged()
        startSearch(query, false)
    }

    override fun onLanguageButtonClicked() {
        onLangButtonClick()
    }

    companion object {
        private const val ARG_QUERY = "lastQuery"
        private const val PANEL_RECENT_SEARCHES = 0
        private const val PANEL_SEARCH_RESULTS = 1
        private const val INTENT_DELAY_MILLIS = 500L
        const val RESULT_LANG_CHANGED = 1
        const val LANG_BUTTON_TEXT_SIZE_LARGER = 12
        const val LANG_BUTTON_TEXT_SIZE_MEDIUM = 10
        const val LANG_BUTTON_TEXT_SIZE_SMALLER = 8

        fun newInstance(source: InvokeSource, query: String?, returnLink: Boolean = false): SearchFragment =
                SearchFragment().apply {
                    arguments = bundleOf(
                        Constants.INTENT_EXTRA_INVOKE_SOURCE to source,
                        ARG_QUERY to query,
                        SearchActivity.EXTRA_RETURN_LINK to returnLink
                    )
                }
    }
}
