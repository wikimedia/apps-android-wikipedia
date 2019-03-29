package org.wikipedia.search;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.IntentFunnel;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.language.LanguageSettingsInvokeSource;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.CabSearchView;
import org.wikipedia.views.LanguageScrollView;
import org.wikipedia.views.ViewUtil;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH;
import static org.wikipedia.settings.languages.WikipediaLanguagesFragment.ACTIVITY_RESULT_LANG_POSITION_DATA;
import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class SearchFragment extends Fragment implements SearchResultsFragment.Callback,
        RecentSearchesFragment.Callback, LanguageScrollView.Callback {

    private static final String ARG_INVOKE_SOURCE = "invokeSource";
    private static final String ARG_QUERY = "lastQuery";

    private static final int PANEL_RECENT_SEARCHES = 0;
    private static final int PANEL_SEARCH_RESULTS = 1;

    @BindView(R.id.search_toolbar) Toolbar toolbar;
    @BindView(R.id.search_cab_view) CabSearchView searchView;
    @BindView(R.id.search_progress_bar) ProgressBar progressBar;
    @BindView(R.id.search_lang_button_container) View langButtonContainer;
    @BindView(R.id.search_lang_button) TextView langButton;
    @BindView(R.id.lang_scroll) LanguageScrollView languageScrollView;
    @BindView(R.id.language_scroll_container) View languageScrollContainer;
    private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();

    private WikipediaApp app;
    private SearchFunnel funnel;
    private SearchInvokeSource invokeSource;
    private String searchLanguageCode;
    private String tempLangCodeHolder;
    private boolean langBtnClicked = false;
    public static final int RESULT_LANG_CHANGED = 1;
    public static final int RESULT_LANG_CONSISTENT = 2;
    public static final int LANG_BUTTON_TEXT_SIZE_LARGER = 12;
    public static final int LANG_BUTTON_TEXT_SIZE_SMALLER = 8;
    /**
     * Whether the Search fragment is currently showing.
     */
    private boolean isSearchActive;

    /**
     * The last search term that the user entered. This will be passed into
     * the TitleSearch and FullSearch sub-fragments.
     */
    @Nullable private String query;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private RecentSearchesFragment recentSearchesFragment;
    private SearchResultsFragment searchResultsFragment;

    private final SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            closeSearch();
            funnel.searchCancel(searchLanguageCode);
            return false;
        }
    };

    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String queryText) {
            PageTitle firstResult = null;
            if (getActivePanel() == PANEL_SEARCH_RESULTS) {
                firstResult = searchResultsFragment.getFirstResult();
            }
            if (firstResult != null) {
                navigateToTitle(firstResult, false, 0);
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String queryText) {
            searchView.setCloseButtonVisibility(queryText);
            startSearch(queryText.trim(), false);
            return true;
        }
    };

    @NonNull public static SearchFragment newInstance(int source,
                                                      @Nullable String query) {
        SearchFragment fragment = new SearchFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_INVOKE_SOURCE, source);
        args.putString(ARG_QUERY, query);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();

        if (savedInstanceState == null) {
            handleIntent(requireActivity().getIntent());
        }

        invokeSource = SearchInvokeSource.of(getArguments().getInt(ARG_INVOKE_SOURCE, SearchInvokeSource.TOOLBAR.code()));
        query = getArguments().getString(ARG_QUERY);
        funnel = new SearchFunnel(app, invokeSource);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = WikipediaApp.getInstance();
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        unbinder = ButterKnife.bind(this, view);

        FragmentManager childFragmentManager = getChildFragmentManager();
        recentSearchesFragment = (RecentSearchesFragment)childFragmentManager.findFragmentById(
                R.id.search_panel_recent);
        recentSearchesFragment.setCallback(this);
        searchResultsFragment = (SearchResultsFragment)childFragmentManager.findFragmentById(
                R.id.fragment_search_results);

        toolbar.setNavigationOnClickListener((v) -> requireActivity().finish());

        initSearchView();

        if (!TextUtils.isEmpty(query)) {
            showPanel(PANEL_SEARCH_RESULTS);
        }

        setUpLanguageScroll(0);
        startSearch(query, false);
        searchView.setCloseButtonVisibility(query);
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH) {
            int position = 0;
            requireActivity().setResult(RESULT_LANG_CHANGED);
            if (data != null && data.hasExtra(ACTIVITY_RESULT_LANG_POSITION_DATA)) {
                position = data.getIntExtra(ACTIVITY_RESULT_LANG_POSITION_DATA, 0);
            } else if (app.language().getAppLanguageCodes().contains(searchLanguageCode)) {
                position = app.language().getAppLanguageCodes().indexOf(searchLanguageCode);
            }
            setUpLanguageScroll(position);
            startSearch(query, true);
        }
    }

    public void handleIntent(Intent intent) {
        IntentFunnel intentFunnel = new IntentFunnel(WikipediaApp.getInstance());
        if (Intent.ACTION_SEND.equals(intent.getAction())
                && Constants.PLAIN_TEXT_MIME_TYPE.equals(intent.getType())) {
            intentFunnel.logShareIntent();
            getArguments().putString(ARG_QUERY, intent.getStringExtra(Intent.EXTRA_TEXT));
            getArguments().putInt(ARG_INVOKE_SOURCE, SearchInvokeSource.INTENT_SHARE.code());
        } else if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())
                && Constants.PLAIN_TEXT_MIME_TYPE.equals(intent.getType())
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intentFunnel.logProcessTextIntent();
            getArguments().putString(ARG_QUERY, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT));
            getArguments().putInt(ARG_INVOKE_SOURCE, SearchInvokeSource.INTENT_PROCESS_TEXT.code());
        }
    }

    private void setUpLanguageScroll(int position) {
        searchLanguageCode = app.language().getAppLanguageCode();

        if (app.language().getAppLanguageCodes().size() > 1) {
            languageScrollContainer.setVisibility(View.VISIBLE);
            languageScrollView.setUpLanguageScrollTabData(app.language().getAppLanguageCodes(), this, position);
            langButtonContainer.setVisibility(View.GONE);
        } else {
            showMultiLingualOnboarding();
            languageScrollContainer.setVisibility(View.GONE);
            langButtonContainer.setVisibility(View.VISIBLE);
            initLangButton();
        }
    }


    private void showMultiLingualOnboarding() {
        if (Prefs.isMultilingualSearchTutorialEnabled()) {
            FeedbackUtil.showTapTargetView(requireActivity(), langButton, R.string.empty,
                    R.string.tool_tip_lang_button, null);
            Prefs.setMultilingualSearchTutorialEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        searchView.setOnCloseListener(null);
        searchView.setOnQueryTextListener(null);
        unbinder.unbind();
        unbinder = null;
        funnel.searchCancel(searchLanguageCode);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        DeviceUtil.setWindowSoftInputModeResizable(requireActivity());
    }

    @Override
    @NonNull
    public SearchFunnel getFunnel() {
        return funnel;
    }

    @Override
    public void switchToSearch(@NonNull String queryText) {
        startSearch(queryText, true);
        searchView.setQuery(queryText, false);
    }

    @Override
    public void onAddLanguageClicked() {
        onLangButtonClick();
    }

    /**
     * Changes the search text box to contain a different string.
     * @param text The text you want to make the search box display.
     */
    @Override
    public void setSearchText(@NonNull CharSequence text) {
        searchView.setQuery(text, false);
    }

    @Override
    public void navigateToTitle(@NonNull PageTitle title, boolean inNewTab, int position) {
        if (!isAdded()) {
            return;
        }
        funnel.searchClick(position, searchLanguageCode);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
        startActivity(inNewTab ? PageActivity.newIntentForNewTab(requireContext(), historyEntry, historyEntry.getTitle())
                : PageActivity.newIntentForExistingTab(requireContext(), historyEntry, historyEntry.getTitle()));
        closeSearch();
    }

    @Override
    public void onSearchResultCopyLink(@NonNull PageTitle title) {
        ClipboardUtil.setPlainText(requireContext(), null, title.getCanonicalUri());
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    @Override
    public void onSearchResultAddToList(@NonNull PageTitle title,
                                        @NonNull AddToReadingListDialog.InvokeSource source) {
        bottomSheetPresenter.show(getChildFragmentManager(), AddToReadingListDialog.newInstance(title, source));
    }

    @Override
    public void onSearchResultShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(requireContext(), title);
    }

    @Override
    public void onSearchProgressBar(boolean enabled) {
        progressBar.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.search_container) void onSearchContainerClick() {
        // Give the root container view an empty click handler, so that click events won't
        // get passed down to any underlying views (e.g. a PageFragment on top of which
        // this fragment is shown)
    }

    @OnClick(R.id.search_lang_button_container)
    void onLangButtonClick() {
        langBtnClicked = true;
        tempLangCodeHolder = searchLanguageCode;
        Intent intent = WikipediaLanguagesActivity.newIntent(requireActivity(), LanguageSettingsInvokeSource.SEARCH.text());
        startActivityForResult(intent, ACTIVITY_REQUEST_ADD_A_LANGUAGE_FROM_SEARCH);
    }

    /**
     * Kick off a search, based on a given search term. Will automatically pass the search to
     * Title search or Full search, based on which one is currently displayed.
     * If the search term is empty, the "recent searches" view will be shown.
     * @param term Phrase to search for.
     * @param force Whether to "force" starting this search. If the search is not forced, the
     *              search may be delayed by a small time, so that network requests are not sent
     *              too often.  If the search is forced, the network request is sent immediately.
     */
    private void startSearch(@Nullable String term, boolean force) {
        if (!isSearchActive) {
            openSearch();
        }

        if (TextUtils.isEmpty(term)) {
            showPanel(PANEL_RECENT_SEARCHES);
        } else if (getActivePanel() == PANEL_RECENT_SEARCHES) {
            //start with title search...
            showPanel(PANEL_SEARCH_RESULTS);
        }

        query = term;

        if (isBlank(term) && !force) {
            return;
        }

        searchResultsFragment.startSearch(term, force);
    }

    /**
     * Activate the Search fragment.
     */
    private void openSearch() {
        // create a new funnel every time Search is opened, to get a new session ID
        funnel = new SearchFunnel(app, invokeSource);
        funnel.searchStart();
        isSearchActive = true;

        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
        // if we already have a previous search query, then put it into the SearchView, and it will
        // automatically trigger the showing of the corresponding search results.
        if (isValidQuery(query)) {
            searchView.setQuery(query, false);
            searchView.selectAllQueryTexts();
        }
    }

    public void closeSearch() {
        isSearchActive = false;
        DeviceUtil.hideSoftKeyboard(getView());
        addRecentSearch(query);
    }

    /**
     * Show a particular panel, which can be one of:
     * - PANEL_RECENT_SEARCHES
     * - PANEL_SEARCH_RESULTS
     * Automatically hides the previous panel.
     *
     * @param panel Which panel to show.
     */
    private void showPanel(int panel) {
        switch (panel) {
            case PANEL_RECENT_SEARCHES:
                searchResultsFragment.hide();
                recentSearchesFragment.show();
                break;
            case PANEL_SEARCH_RESULTS:
                recentSearchesFragment.hide();
                searchResultsFragment.show();
                break;
            default:
                break;
        }
    }

    private int getActivePanel() {
        if (searchResultsFragment.isShowing()) {
            return PANEL_SEARCH_RESULTS;
        } else {
            //otherwise, the recent searches must be showing:
            return PANEL_RECENT_SEARCHES;
        }
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setOnCloseListener(searchCloseListener);
        searchView.setSearchHintTextColor(getThemedColor(requireContext(), R.attr.material_theme_de_emphasised_color));

        // remove focus line from search plate
        View searchEditPlate = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);
    }

    private void initLangButton() {
        langButton.setText(app.language().getAppLanguageCode().toUpperCase(Locale.ENGLISH));
        ViewUtil.formatLangButton(langButton, app.language().getAppLanguageCode().toUpperCase(Locale.ENGLISH), LANG_BUTTON_TEXT_SIZE_SMALLER, LANG_BUTTON_TEXT_SIZE_LARGER);
        FeedbackUtil.setToolbarButtonLongPressToast(langButtonContainer);
    }

    private boolean isValidQuery(String queryText) {
        return queryText != null && TextUtils.getTrimmedLength(queryText) > 0;
    }

    private void addRecentSearch(String title) {
        if (isValidQuery(title)) {
            disposables.add(Completable.fromAction(() -> app.getDatabaseClient(RecentSearch.class).upsert(new RecentSearch(title), SearchHistoryContract.Query.SELECTION))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> recentSearchesFragment.updateList(),
                            L::e));
        }
    }

    @Override
    public void onLanguageTabSelected(String selectedLanguageCode) {
        if (langBtnClicked) {
            //We need to skip an event when we return back from 'add languages' screen,
            // because it triggers two events while re-drawing the UI
            langBtnClicked = false;
        } else {
            //We need a temporary language code holder because the previously selected search language code[searchLanguageCode]
            // gets overwritten when UI is re-drawn
            funnel.searchLanguageSwitch(!TextUtils.isEmpty(tempLangCodeHolder) && !tempLangCodeHolder.equals(selectedLanguageCode) ? tempLangCodeHolder : searchLanguageCode, selectedLanguageCode);
            tempLangCodeHolder = null;
        }
        searchLanguageCode = selectedLanguageCode;
        searchResultsFragment.setLayoutDirection(searchLanguageCode);
        startSearch(query, true);
    }

    @Override
    public void onLanguageButtonClicked() {
        onLangButtonClick();
    }

    public String getSearchLanguageCode() {
        return searchLanguageCode;
    }
}
