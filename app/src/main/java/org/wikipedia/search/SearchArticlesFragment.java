package org.wikipedia.search;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.history.HistoryEntryDatabaseTable;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.settings.LanguagePreferenceDialog;

import com.squareup.otto.Subscribe;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class SearchArticlesFragment extends Fragment implements BackPressedHandler {
    private static final String ARG_LAST_SEARCHED_TEXT = "lastSearchedText";
    private static final String ARG_SEARCH_CURRENT_PANEL = "searchCurrentPanel";

    private static final int PANEL_RECENT_SEARCHES = 0;
    private static final int PANEL_SEARCH_RESULTS = 1;

    private WikipediaApp app;
    private SearchView searchView;
    private EditText searchEditText;
    private SearchFunnel funnel;
    private TextView langButton;

    public SearchFunnel getFunnel() {
        return funnel;
    }

    private boolean launchedFromWidget = false;
    public void setLaunchedFromWidget(boolean fromWidget) {
        launchedFromWidget = fromWidget;
    }
    public boolean isLaunchedFromWidget() {
        return launchedFromWidget;
    }

    /**
     * Whether the Search fragment is currently showing.
     */
    private boolean isSearchActive = false;

    /**
     * The last search term that the user entered. This will be passed into
     * the TitleSearch and FullSearch sub-fragments.
     */
    private String lastSearchedText;

    /**
     * View that contains the whole Search fragment. This is what should be shown/hidden when
     * the search is called for from the main activity.
     */
    private View searchContainerView;

    private RecentSearchesFragment recentSearchesFragment;
    private SearchResultsFragment searchResultsFragment;

    public SearchArticlesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        funnel = new SearchFunnel(WikipediaApp.getInstance());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (WikipediaApp) getActivity().getApplicationContext();
        app.getBus().register(this);
        View parentLayout = inflater.inflate(R.layout.fragment_search, container, false);

        searchContainerView = parentLayout.findViewById(R.id.search_container);
        searchContainerView.setPadding(0, getContentTopOffsetPx(getActivity()), 0, 0);
        searchContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Give the root container view an empty click handler, so that click events won't
                // get passed down to any underlying views (e.g. a PageFragment on top of which
                // this fragment is shown)
            }
        });

        View deleteButton = parentLayout.findViewById(R.id.recent_searches_delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setMessage(getString(R.string.clear_recent_searches_confirm));
                alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new DeleteAllRecentSearchesTask(app).execute();
                    }
                });
                alert.setNegativeButton(getString(R.string.no), null);
                alert.create().show();
            }
        });

        recentSearchesFragment = (RecentSearchesFragment)getChildFragmentManager().findFragmentById(R.id.search_panel_recent);

        searchResultsFragment = (SearchResultsFragment)getChildFragmentManager().findFragmentById(R.id.fragment_search_results);

        // make sure we're hidden by default
        searchContainerView.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            lastSearchedText = savedInstanceState.getString(ARG_LAST_SEARCHED_TEXT);
            showPanel(savedInstanceState.getInt(ARG_SEARCH_CURRENT_PANEL));
        }
        return parentLayout;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        app.getBus().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_LAST_SEARCHED_TEXT, lastSearchedText);
        outState.putInt(ARG_SEARCH_CURRENT_PANEL, getActivePanel());
    }

    public void switchToSearch(String queryText) {
        startSearch(queryText, true);
        searchView.setQuery(queryText, false);
    }

    /**
     * Changes the search text box to contain a different string.
     * @param text The text you want to make the search box display.
     */
    public void setSearchText(CharSequence text) {
        searchView.setQuery(text, false);
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

    @Subscribe
    public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
        updateZeroChrome();
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
    public void startSearch(String term, boolean force) {
        if (!isSearchActive) {
            openSearch();
        }

        if (TextUtils.isEmpty(term)) {
            showPanel(PANEL_RECENT_SEARCHES);
        } else if (getActivePanel() == PANEL_RECENT_SEARCHES) {
            //start with title search...
            showPanel(PANEL_SEARCH_RESULTS);
        }

        lastSearchedText = term;

        searchResultsFragment.startSearch(term, force);
    }

    /**
     * Activate the Search fragment.
     */
    public void openSearch() {
        // create a new funnel every time Search is opened, to get a new session ID
        funnel = new SearchFunnel(WikipediaApp.getInstance());
        funnel.searchStart();
        isSearchActive = true;
        // invalidate our activity's ActionBar, so that all action items are removed, and
        // we can fill up the whole width of the ActionBar with our SearchView.
        getActivity().supportInvalidateOptionsMenu();
        ((PageActivity)getActivity()).getSearchBarHideHandler().setForceNoFade(true);
        setSearchViewEnabled(true);
        ((PageActivity) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
        // show ourselves
        searchContainerView.setVisibility(View.VISIBLE);

        // if the current search string is empty, then it's a fresh start, so we'll show
        // recent searches by default. Otherwise, the currently-selected panel should already
        // be visible, so we don't need to do anything.
        if (TextUtils.isEmpty(lastSearchedText)) {
            showPanel(PANEL_RECENT_SEARCHES);
        }
    }

    public void closeSearch() {
        isSearchActive = false;
        // invalidate our activity's ActionBar, so that the original action items are restored.
        getActivity().supportInvalidateOptionsMenu();
        ((PageActivity)getActivity()).getSearchBarHideHandler().setForceNoFade(false);
        setSearchViewEnabled(false);
        ((PageActivity) getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
        // hide ourselves
        searchContainerView.setVisibility(View.GONE);
        hideSoftKeyboard(getActivity());
        addRecentSearch(lastSearchedText);
    }

    /**
     * Determine whether the Search fragment is currently active.
     * @return Whether the Search fragment is active.
     */
    public boolean isSearchActive() {
        return isSearchActive;
    }

    public boolean onBackPressed() {
        if (isSearchActive) {
            closeSearch();
            funnel.searchCancel();
            return true;
        }
        return false;
    }

    private void setSearchViewEnabled(boolean enabled) {
        LinearLayout enabledSearchBar = (LinearLayout) getActivity().findViewById(R.id.search_bar_enabled);
        TextView searchButton = (TextView) getActivity().findViewById(R.id.main_search_bar_text);
        langButton = (TextView) getActivity().findViewById(R.id.search_lang_button);
        FrameLayout langButtonContainer = (FrameLayout) getActivity().findViewById(R.id.search_lang_button_container);

        if (enabled) {
            // set up the language picker
            langButton.setText(app.getAppOrSystemLanguageCode().toUpperCase());
            formatLangButtonText();
            langButtonContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLangPreferenceDialog();
                }
            });

            // set up the SearchView
            if (searchView == null) {
                searchView = (SearchView)getActivity().findViewById(R.id.main_search_view);
                searchView.setOnQueryTextListener(searchQueryListener);
                searchView.setOnCloseListener(searchCloseListener);

                searchEditText = (EditText) searchView
                        .findViewById(android.support.v7.appcompat.R.id.search_src_text);
                // make the text size be the same as the size of the search field
                // placeholder in the main activity
                searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_PX, searchButton.getTextSize());
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
            }

            updateZeroChrome();
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();

            // if we already have a previous search query, then put it into the SearchView, and it will
            // automatically trigger the showing of the corresponding search results.
            if (isValidQuery(lastSearchedText)) {
                searchView.setQuery(lastSearchedText, false);
                // automatically select all text in the search field, so that typing a new character
                // will clear it by default
                if (searchEditText != null) {
                    searchEditText.selectAll();
                }
            }
            searchButton.setVisibility(View.GONE);
            enabledSearchBar.setVisibility(View.VISIBLE);
        } else {
            enabledSearchBar.setVisibility(View.GONE);
            searchButton.setVisibility(View.VISIBLE);
        }
    }

    /*
    Update any UI elements related to WP Zero
     */
    private void updateZeroChrome() {
        if (searchEditText != null) {
            // setting the hint directly on the search EditText (instead of the SearchView)
            // gets rid of the magnify icon, which we don't want.
            searchEditText.setHint(app.getWikipediaZeroHandler().isZeroEnabled() ? getString(
                    R.string.zero_search_hint) : getString(R.string.search_hint));
        }
    }

    private boolean isValidQuery(String queryText) {
        return queryText != null && TextUtils.getTrimmedLength(queryText) > 0;
    }

    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String queryText) {
            PageTitle firstResult = null;
            if (getActivePanel() == PANEL_SEARCH_RESULTS) {
                firstResult = searchResultsFragment.getFirstResult();
            }
            if (firstResult != null) {
                navigateToTitle(firstResult, false);
                closeSearch();
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String queryText) {
            startSearch(queryText.trim(), false);
            return true;
        }
    };

    private final SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            getActivity().onBackPressed();
            return false;
        }
    };

    public void navigateToTitle(PageTitle title, boolean inNewTab) {
        if (!isAdded()) {
            return;
        }
        funnel.searchClick();
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
        hideSoftKeyboard(getActivity());
        // if this search session was started from our Widget, then clear the widget flag,
        // so that we no longer care that we were launched from the widget, since we've now
        // selected a page to navigate to.
        launchedFromWidget = false;
        closeSearch();
        ((PageActivity)getActivity()).loadPage(title, historyEntry, inNewTab
                ? PageActivity.TabPosition.NEW_TAB_BACKGROUND
                : PageActivity.TabPosition.CURRENT_TAB, false);
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
            app.getDatabaseClient(RecentSearch.class).upsert(entry, HistoryEntryDatabaseTable.SELECTION_KEYS);
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
                langButton.setText(langCode.substring(0, langButtonTextMaxLength).toUpperCase());
            }
            return;
        }
        langButton.setTextSize(langButtonTextSizeLarger);
    }

    public void showLangPreferenceDialog() {
        LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(getActivity(), true);
        langPrefDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                langButton.setText(app.getAppOrSystemLanguageCode().toUpperCase());
                formatLangButtonText();
                if (!TextUtils.isEmpty(lastSearchedText)) {
                    startSearch(lastSearchedText, true);
                }
            }
        });
        langPrefDialog.show();
    }
}
