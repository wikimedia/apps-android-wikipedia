package org.wikipedia.search;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.settings.LanguagePreferenceDialog;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.ViewUtil;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SearchFragment extends Fragment implements BackPressedHandler,
        SearchResultsFragment.Callback, RecentSearchesFragment.Parent {

    public interface Callback {
        void onSearchSelectPage(@NonNull HistoryEntry entry, boolean inNewTab);
        void onSearchOpen();
        void onSearchClose(boolean launchedFromIntent);
        void onSearchResultCopyLink(@NonNull PageTitle title);
        void onSearchResultAddToList(@NonNull PageTitle title,
                                     @NonNull AddToReadingListDialog.InvokeSource source);
        void onSearchResultShareLink(@NonNull PageTitle title);
    }

    private static final String ARG_INVOKE_SOURCE = "invokeSource";
    private static final String ARG_QUERY = "lastQuery";

    private static final int PANEL_RECENT_SEARCHES = 0;
    private static final int PANEL_SEARCH_RESULTS = 1;

    @BindView(R.id.search_container) View searchContainer;
    @BindView(R.id.search_toolbar) Toolbar toolbar;
    @BindView(R.id.search_cab_view) SearchView searchView;
    @BindView(R.id.search_progress_bar) ProgressBar progressBar;
    @BindView(R.id.search_lang_button_container) View langButtonContainer;
    @BindView(R.id.search_lang_button) TextView langButton;
    @BindView(R.id.search_offline_library_state) View offlineLibraryStateView;
    private Unbinder unbinder;

    private WikipediaApp app;
    @BindView(android.support.v7.appcompat.R.id.search_src_text) EditText searchEditText;
    private SearchFunnel funnel;
    private SearchInvokeSource invokeSource;

    /**
     * Whether the Search fragment is currently showing.
     */
    private boolean isSearchActive;

    /**
     * The last search term that the user entered. This will be passed into
     * the TitleSearch and FullSearch sub-fragments.
     */
    @Nullable private String query;

    private RecentSearchesFragment recentSearchesFragment;
    private SearchResultsFragment searchResultsFragment;

    private final SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            closeSearch();
            funnel.searchCancel();
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
            startSearch(queryText.trim(), false);
            return true;
        }
    };

    @NonNull public static SearchFragment newInstance(@NonNull SearchInvokeSource source,
                                                      @Nullable String query) {
        SearchFragment fragment = new SearchFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_INVOKE_SOURCE, source.code());
        args.putString(ARG_QUERY, query);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();

        invokeSource = SearchInvokeSource.of(getArguments().getInt(ARG_INVOKE_SOURCE));
        query = getArguments().getString(ARG_QUERY);

        funnel = new SearchFunnel(app, invokeSource);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = WikipediaApp.getInstance();
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        unbinder = ButterKnife.bind(this, view);

        FragmentManager childFragmentManager = getChildFragmentManager();
        recentSearchesFragment = (RecentSearchesFragment)childFragmentManager.findFragmentById(
                R.id.search_panel_recent);
        searchResultsFragment = (SearchResultsFragment)childFragmentManager.findFragmentById(
                R.id.fragment_search_results);

        toolbar.setNavigationOnClickListener((v) -> onBackPressed());

        initSearchView();
        initLangButton();
        updateOfflineLibraryState();

        if (!TextUtils.isEmpty(query)) {
            showPanel(PANEL_SEARCH_RESULTS);
        }

        startSearch(query, false);

        return view;
    }

    @Override
    public void onDestroyView() {
        searchView.setOnCloseListener(null);
        searchView.setOnQueryTextListener(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    @NonNull
    public SearchFunnel getFunnel() {
        return funnel;
    }

    public boolean isLaunchedFromIntent() {
        return invokeSource.fromIntent();
    }

    @Override
    public void switchToSearch(@NonNull String queryText) {
        startSearch(queryText, true);
        searchView.setQuery(queryText, false);
    }

    /**
     * Changes the search text box to contain a different string.
     * @param text The text you want to make the search box display.
     */
    @Override
    public void setSearchText(@NonNull CharSequence text) {
        searchView.setQuery(text, false);
    }

    /**
     * Determine whether the Search fragment is currently active.
     * @return Whether the Search fragment is active.
     */
    public boolean isSearchActive() {
        return isSearchActive;
    }

    @Override
    public boolean onBackPressed() {
        if (isSearchActive) {
            // todo: activity or fragment transition
            closeSearch();
            funnel.searchCancel();
            return true;
        }
        return false;
    }

    @Override
    public void navigateToTitle(@NonNull PageTitle title, boolean inNewTab, int position) {
        if (!isAdded()) {
            return;
        }
        funnel.searchClick(position);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchSelectPage(historyEntry, inNewTab);
        }
        closeSearch();
    }

    @Override
    public void onSearchResultCopyLink(@NonNull PageTitle title) {
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchResultCopyLink(title);
        }
    }

    @Override
    public void onSearchResultAddToList(@NonNull PageTitle title,
                                        @NonNull AddToReadingListDialog.InvokeSource source) {
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchResultAddToList(title, source);
        }
    }

    @Override
    public void onSearchResultShareLink(@NonNull PageTitle title) {
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchResultShareLink(title);
        }
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

    @OnClick(R.id.search_lang_button_container) void onLangButtonClick() {
        showLangPreferenceDialog();
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

        updateOfflineLibraryState();
        searchResultsFragment.startSearch(term, force);
    }

    /**
     * Activate the Search fragment.
     */
    private void openSearch() {
        // create a new funnel every time Search is opened, to get a new session ID
        funnel = new SearchFunnel(WikipediaApp.getInstance(), invokeSource);
        funnel.searchStart();
        isSearchActive = true;
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchOpen();
        }
        // show ourselves
        ViewUtil.fadeIn(searchContainer);

        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
        // if we already have a previous search query, then put it into the SearchView, and it will
        // automatically trigger the showing of the corresponding search results.
        if (isValidQuery(query)) {
            searchView.setQuery(query, false);
            searchEditText.selectAll();
        }
    }

    public void closeSearch() {
        isSearchActive = false;
        // hide ourselves
        ViewUtil.fadeOut(searchContainer);
        DeviceUtil.hideSoftKeyboard(getView());
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchClose(invokeSource.fromIntent());
        }
        addRecentSearch(query);
    }

    private void updateOfflineLibraryState() {
        offlineLibraryStateView.setVisibility(
                (OfflineManager.hasCompilation() && !DeviceUtil.isOnline())
                        ? View.VISIBLE : View.GONE);
    }

    /**
     * Show a particular panel, which can be one of:
     * - PANEL_RECENT_SEARCHES
     * - PANEL_SEARCH_RESULTS
     * Automatically hides the previous panel.
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

        // reset its background
        searchEditText.setBackgroundColor(Color.TRANSPARENT);
        // make the search frame match_parent
        View searchEditFrame = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_edit_frame);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        searchEditFrame.setLayoutParams(params);
        // center the search text in it
        searchEditText.setGravity(Gravity.CENTER_VERTICAL);
        // remove focus line from search plate
        View searchEditPlate = searchView
                .findViewById(android.support.v7.appcompat.R.id.search_plate);
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT);

        ImageView searchClose = searchView.findViewById(
                android.support.v7.appcompat.R.id.search_close_btn);
        FeedbackUtil.setToolbarButtonLongPressToast(searchClose);
    }

    private void initLangButton() {
        langButton.setText(app.getAppOrSystemLanguageCode().toUpperCase(Locale.ENGLISH));
        formatLangButtonText();
        FeedbackUtil.setToolbarButtonLongPressToast(langButtonContainer);
    }

    private boolean isValidQuery(String queryText) {
        return queryText != null && TextUtils.getTrimmedLength(queryText) > 0;
    }

    private void addRecentSearch(String title) {
        if (isValidQuery(title)) {
            new SaveRecentSearchTask(new RecentSearch(title)).execute();
        }
    }

    private final class SaveRecentSearchTask extends SaneAsyncTask<Void> {
        private final RecentSearch entry;
        SaveRecentSearchTask(RecentSearch entry) {
            this.entry = entry;
        }

        @Override
        public Void performTask() throws Throwable {
            app.getDatabaseClient(RecentSearch.class).upsert(entry, SearchHistoryContract.Query.SELECTION);
            return null;
        }

        @Override
        public void onFinish(Void result) {
            super.onFinish(result);
            recentSearchesFragment.updateList();
        }

        @Override
        public void onCatch(Throwable caught) {
            Log.w("SaveRecentSearchTask", "Caught " + caught.getMessage(), caught);
        }
    }

    private void formatLangButtonText() {
        final int langCodeStandardLength = 3;
        final int langButtonTextMaxLength = 7;

        // These values represent scaled pixels (sp)
        final int langButtonTextSizeSmaller = 10;
        final int langButtonTextSizeLarger = 13;

        String langCode = app.getAppOrSystemLanguageCode();
        if (langCode.length() > langCodeStandardLength) {
            langButton.setTextSize(langButtonTextSizeSmaller);
            if (langCode.length() > langButtonTextMaxLength) {
                langButton.setText(langCode.substring(0, langButtonTextMaxLength).toUpperCase(Locale.ENGLISH));
            }
            return;
        }
        langButton.setTextSize(langButtonTextSizeLarger);
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private void showLangPreferenceDialog() {
        LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(getContext(), true);
        langPrefDialog.setOnDismissListener((dialog) -> {
            if (getActivity() == null) {
                return;
            }

            langButton.setText(app.getAppOrSystemLanguageCode().toUpperCase(Locale.ENGLISH));
            formatLangButtonText();
            if (!TextUtils.isEmpty(query)) {
                startSearch(query, true);
            }
        });
        langPrefDialog.show();
    }
}
