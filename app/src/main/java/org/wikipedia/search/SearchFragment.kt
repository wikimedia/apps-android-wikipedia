package org.wikipedia.search

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.IntentFunnel
import org.wikipedia.analytics.SearchFunnel
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentSearchBinding
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil.AddToDefaultListCallback
import org.wikipedia.readinglist.ReadingListBehaviorsUtil.addToDefaultList
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.settings.languages.WikipediaLanguagesFragment
import org.wikipedia.util.DeviceUtil.hideSoftKeyboard
import org.wikipedia.util.DeviceUtil.setWindowSoftInputModeResizable
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.FeedbackUtil.showTooltip
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.StringUtil.listToJsonArrayString
import org.wikipedia.views.LanguageScrollView
import org.wikipedia.views.ViewUtil.formatLangButton
import java.util.*

class SearchFragment : Fragment(), SearchResultsFragment.Callback, RecentSearchesFragment.Callback, LanguageScrollView.Callback {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val disposables = CompositeDisposable()
    private var app = WikipediaApp.getInstance()
    private var tempLangCodeHolder: String? = null
    private var langBtnClicked = false
    private var isSearchActive = false
    private var query: String? = null
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private lateinit var recentSearchesFragment: RecentSearchesFragment
    private lateinit var searchResultsFragment: SearchResultsFragment
    private lateinit var funnel: SearchFunnel
    private lateinit var invokeSource: InvokeSource
    private lateinit var initialLanguageList: String
    var searchLanguageCode = app.language().appLanguageCode
        private set

    private val searchCloseListener = SearchView.OnCloseListener {
        closeSearch()
        funnel.searchCancel(searchLanguageCode)
        false
    }

    private val searchQueryListener: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(queryText: String): Boolean {
            hideSoftKeyboard(requireActivity())
            return true
        }

        override fun onQueryTextChange(queryText: String): Boolean {
            binding.searchCabView.setCloseButtonVisibility(queryText)
            startSearch(queryText.trim(), false)
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(requireActivity().intent)
        }
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        query = requireArguments().getString(ARG_QUERY)
        funnel = SearchFunnel(app, invokeSource)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val childFragmentManager = childFragmentManager
        recentSearchesFragment = childFragmentManager.findFragmentById(
                R.id.search_panel_recent) as RecentSearchesFragment
        recentSearchesFragment.callback = this
        searchResultsFragment = childFragmentManager.findFragmentById(
                R.id.fragment_search_results) as SearchResultsFragment
        binding.searchToolbar.setNavigationOnClickListener { requireActivity().supportFinishAfterTransition() }
        initialLanguageList = listToJsonArrayString(app.language().appLanguageCodes)
        binding.searchContainer.setOnClickListener { onSearchContainerClick() }
        binding.searchLangButtonContainer.setOnClickListener { onLangButtonClick() }
        initSearchView()
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        setUpLanguageScroll(Prefs.getSelectedLanguagePositionInSearch())
        startSearch(query, langBtnClicked)
        binding.searchCabView.setCloseButtonVisibility(query)
        if (!query.isNullOrEmpty()) {
            showPanel(PANEL_SEARCH_RESULTS)
        }
    }

    override fun onPause() {
        super.onPause()
        Prefs.setSelectedLanguagePositionInSearch(binding.searchLanguageScrollView.selectedPosition)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH) {
            var position = 0
            val finalLanguageList = listToJsonArrayString(app.language().appLanguageCodes)
            if (finalLanguageList != initialLanguageList) {
                requireActivity().setResult(RESULT_LANG_CHANGED)
            }
            if (data != null && data.hasExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                position = data.getIntExtra(WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA, 0)
            } else if (app.language().appLanguageCodes.contains(searchLanguageCode)) {
                position = app.language().appLanguageCodes.indexOf(searchLanguageCode)
            }
            searchResultsFragment.clearSearchResultsCountCache()
            Prefs.setSelectedLanguagePositionInSearch(position)
        }
    }

    private fun handleIntent(intent: Intent) {
        val intentFunnel = IntentFunnel(WikipediaApp.getInstance())
        if (Intent.ACTION_SEND == intent.action && Constants.PLAIN_TEXT_MIME_TYPE == intent.type) {
            intentFunnel.logShareIntent()
            requireArguments().putString(ARG_QUERY, intent.getStringExtra(Intent.EXTRA_TEXT))
            requireArguments().putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.INTENT_SHARE)
        } else if (Intent.ACTION_PROCESS_TEXT == intent.action && Constants.PLAIN_TEXT_MIME_TYPE ==
                intent.type && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intentFunnel.logProcessTextIntent()
            requireArguments().putString(ARG_QUERY, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT))
            requireArguments().putSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE, InvokeSource.INTENT_PROCESS_TEXT)
        }
    }

    fun setUpLanguageScroll(position: Int) {
        var pos = position
        if (app.language().appLanguageCodes.size > 1) {
            pos = if (app.language().appLanguageCodes.size > pos) pos else 0
            binding.searchLanguageScrollViewContainer.visibility = View.VISIBLE
            binding.searchLanguageScrollView.setUpLanguageScrollTabData(app.language().appLanguageCodes, pos, this)
            binding.searchLangButtonContainer.visibility = View.GONE
        } else {
            showMultiLingualOnboarding()
            binding.searchLanguageScrollViewContainer.visibility = View.GONE
            binding.searchLangButtonContainer.visibility = View.VISIBLE
            initLangButton()
        }
    }

    private fun showMultiLingualOnboarding() {
        if (Prefs.isMultilingualSearchTutorialEnabled()) {
            binding.searchLangButton.postDelayed({
                if (isAdded) {
                    showTooltip(requireActivity(), binding.searchLangButton, getString(R.string.tool_tip_lang_button),
                            aboveOrBelow = false, autoDismiss = false)
                }
            }, 500)
            Prefs.setMultilingualSearchTutorialEnabled(false)
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.searchCabView.setOnCloseListener(null)
        binding.searchCabView.setOnQueryTextListener(null)
        _binding = null
        funnel.searchCancel(searchLanguageCode)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        setWindowSoftInputModeResizable(requireActivity())
    }

    override fun getFunnel(): SearchFunnel {
        return funnel
    }

    override fun switchToSearch(text: String) {
        startSearch(text, true)
        binding.searchCabView.setQuery(text, false)
    }

    override fun onAddLanguageClicked() {
        onLangButtonClick()
    }

    override fun setSearchText(text: CharSequence) {
        binding.searchCabView.setQuery(text, false)
    }

    override fun navigateToTitle(item: PageTitle, inNewTab: Boolean, position: Int) {
        if (!isAdded) {
            return
        }
        funnel.searchClick(position, searchLanguageCode)
        val historyEntry = HistoryEntry(item, HistoryEntry.SOURCE_SEARCH)
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(requireContext(), historyEntry, historyEntry.title)
        else PageActivity.newIntentForCurrentTab(requireContext(), historyEntry, historyEntry.title, false))
        closeSearch()
    }

    override fun onSearchAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
        if (addToDefault) {
            addToDefaultList(requireActivity(), entry.title, InvokeSource.FEED, object : AddToDefaultListCallback {
                override fun onMoveClicked(readingListId: Long) {
                    onSearchMovePageToList(readingListId, entry)
                }
            })
        } else {
            bottomSheetPresenter.show(childFragmentManager,
                    AddToReadingListDialog.newInstance(entry.title, InvokeSource.FEED))
        }
    }

    override fun onSearchMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
        bottomSheetPresenter.show(childFragmentManager,
                MoveToReadingListDialog.newInstance(sourceReadingListId, entry.title, InvokeSource.FEED))
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
        val intent = WikipediaLanguagesActivity.newIntent(requireActivity(), InvokeSource.SEARCH)
        startActivityForResult(intent, Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH)
    }

    private fun startSearch(term: String?, force: Boolean) {
        if (!isSearchActive) {
            openSearch()
        }
        if (term.isNullOrEmpty()) {
            showPanel(PANEL_RECENT_SEARCHES)
        } else if (activePanel == PANEL_RECENT_SEARCHES) {
            // start with title search...
            showPanel(PANEL_SEARCH_RESULTS)
        }
        query = term
        if (term.isNullOrBlank() && !force) {
            return
        }
        searchResultsFragment.startSearch(term, force)
    }

    private fun openSearch() {
        // create a new funnel every time Search is opened, to get a new session ID
        funnel = SearchFunnel(app, invokeSource)
        funnel.searchStart()
        isSearchActive = true
        binding.searchCabView.isIconified = false
        // if we already have a previous search query, then put it into the SearchView, and it will
        // automatically trigger the showing of the corresponding search results.
        if (isValidQuery(query)) {
            binding.searchCabView.setQuery(query, false)
            binding.searchCabView.selectAllQueryTexts()
        }
    }

    private fun closeSearch() {
        isSearchActive = false
        hideSoftKeyboard(requireView())
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
        binding.searchCabView.setSearchHintTextColor(getThemedColor(requireContext(),
                R.attr.material_theme_de_emphasised_color))

        // remove focus line from search plate
        val searchEditPlate = binding.searchCabView
                .findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun initLangButton() {
        binding.searchLangButton.text = app.language().appLanguageCode.toUpperCase(Locale.ENGLISH)
        formatLangButton(binding.searchLangButton, app.language().appLanguageCode.toUpperCase(Locale.ENGLISH),
                LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER)
        setButtonLongPressToast(binding.searchLangButtonContainer)
    }

    private fun isValidQuery(queryText: String?): Boolean {
        return queryText != null && queryText.trim().isNotEmpty()
    }

    private fun addRecentSearch(title: String?) {
        if (isValidQuery(title)) {
            disposables.add(Completable.fromAction {
                AppDatabase.getAppDatabase().recentSearchDao().insertRecentSearch(RecentSearch(title))
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ recentSearchesFragment.updateList() }) { obj: Throwable -> obj.printStackTrace() })
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
            funnel.searchLanguageSwitch((if (!tempLangCodeHolder.isNullOrEmpty() && tempLangCodeHolder != selectedLanguageCode)
                tempLangCodeHolder else searchLanguageCode)!!, selectedLanguageCode)
            tempLangCodeHolder = null
        }
        searchLanguageCode = selectedLanguageCode
        searchResultsFragment.setLayoutDirection(searchLanguageCode)
        startSearch(query, true)
    }

    override fun onLanguageButtonClicked() {
        onLangButtonClick()
    }

    companion object {
        private const val ARG_QUERY = "lastQuery"
        private const val PANEL_RECENT_SEARCHES = 0
        private const val PANEL_SEARCH_RESULTS = 1
        const val RESULT_LANG_CHANGED = 1
        const val LANG_BUTTON_TEXT_SIZE_LARGER = 12
        const val LANG_BUTTON_TEXT_SIZE_SMALLER = 8

        @JvmStatic
        fun newInstance(source: InvokeSource, query: String?): SearchFragment =
                SearchFragment().apply {
                    arguments = bundleOf(
                        Constants.INTENT_EXTRA_INVOKE_SOURCE to source,
                        ARG_QUERY to query
                    )
                }
    }
}
