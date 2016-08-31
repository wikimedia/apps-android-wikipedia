package org.wikipedia.search;

import android.content.DialogInterface;
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

import com.squareup.otto.Subscribe;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.LanguagePreferenceDialog;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class SearchFragment extends Fragment implements BackPressedHandler,
        SearchResultsFragment.Parent, RecentSearchesFragment.Parent {
    public enum InvokeSource implements EnumCode {
        TOOLBAR(0),
        WIDGET(1),
        INTENT_SHARE(2),
        INTENT_PROCESS_TEXT(3),
        FEED_BAR(4),
        VOICE(5);

        private static final EnumCodeMap<InvokeSource> MAP = new EnumCodeMap<>(InvokeSource.class);

        private final int code;

        public static InvokeSource of(int code) {
            return MAP.get(code);
        }

        @Override public int code() {
            return code;
        }

        InvokeSource(int code) {
            this.code = code;
        }

        public boolean fromIntent() {
            return code == WIDGET.code() || code == INTENT_SHARE.code()
                    || code == INTENT_PROCESS_TEXT.code();
        }
    }

    public interface Callback {
        void onSearchSelectPage(@NonNull HistoryEntry entry, boolean inNewTab);
        void onSearchOpen();
        void onSearchClose();
    }

    private static final String ARG_LAST_SEARCHED_TEXT = "lastSearchedText";
    private static final String ARG_SEARCH_CURRENT_PANEL = "searchCurrentPanel";
    private static final String ARG_INVOKE_SOURCE = "invokeSource";

    private static final int PANEL_RECENT_SEARCHES = 0;
    private static final int PANEL_SEARCH_RESULTS = 1;

    @BindView(R.id.search_container) View searchContainer;
    @BindView(R.id.search_toolbar) Toolbar toolbar;
    @BindView(R.id.search_cab_view) SearchView searchView;
    @BindView(R.id.search_progress_bar) ProgressBar progressBar;
    @BindView(R.id.search_lang_button_container) View langButtonContainer;
    @BindView(R.id.search_lang_button) TextView langButton;
    private Unbinder unbinder;

    private WikipediaApp app;
    private EditText searchEditText;
    private SearchFunnel funnel;
    private InvokeSource invokeSource = InvokeSource.TOOLBAR;

    /**
     * Whether the Search fragment is currently showing.
     */
    private boolean isSearchActive = false;

    /**
     * The last search term that the user entered. This will be passed into
     * the TitleSearch and FullSearch sub-fragments.
     */
    private String lastSearchedText;

    private RecentSearchesFragment recentSearchesFragment;
    private SearchResultsFragment searchResultsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        funnel = new SearchFunnel(WikipediaApp.getInstance(), InvokeSource.of(invokeSource.code()));
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

        searchContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Give the root container view an empty click handler, so that click events won't
                // get passed down to any underlying views (e.g. a PageFragment on top of which
                // this fragment is shown)
            }
        });

        FragmentManager childFragmentManager = getChildFragmentManager();
        recentSearchesFragment = (RecentSearchesFragment)childFragmentManager.findFragmentById(
                R.id.search_panel_recent);
        searchResultsFragment = (SearchResultsFragment)childFragmentManager.findFragmentById(
                R.id.fragment_search_results);

        // make sure we're hidden by default
        searchContainer.setVisibility(View.GONE);

        if (savedInstanceState != null) {
            lastSearchedText = savedInstanceState.getString(ARG_LAST_SEARCHED_TEXT);
            invokeSource = InvokeSource.of(savedInstanceState.getInt(ARG_INVOKE_SOURCE));
            showPanel(savedInstanceState.getInt(ARG_SEARCH_CURRENT_PANEL));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        initSearchView();
        initLangButton();
        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_LAST_SEARCHED_TEXT, lastSearchedText);
        outState.putInt(ARG_SEARCH_CURRENT_PANEL, getActivePanel());
        outState.putInt(ARG_INVOKE_SOURCE, invokeSource.code());
    }

    @Override
    @NonNull
    public SearchFunnel getFunnel() {
        return funnel;
    }

    public void setInvokeSource(InvokeSource source) {
        invokeSource = source;
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
        funnel = new SearchFunnel(WikipediaApp.getInstance(), InvokeSource.of(invokeSource.code()));
        funnel.searchStart();
        isSearchActive = true;
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchOpen();
        }
        // show ourselves
        ViewUtil.fadeIn(searchContainer);

        // if the current search string is empty, then it's a fresh start, so we'll show
        // recent searches by default. Otherwise, the currently-selected panel should already
        // be visible, so we don't need to do anything.
        if (TextUtils.isEmpty(lastSearchedText)) {
            showPanel(PANEL_RECENT_SEARCHES);
        }

        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
        // if we already have a previous search query, then put it into the SearchView, and it will
        // automatically trigger the showing of the corresponding search results.
        if (isValidQuery(lastSearchedText)) {
            searchView.setQuery(lastSearchedText, false);
            searchEditText.selectAll();
        }
    }

    public void closeSearch() {
        isSearchActive = false;
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchClose();
        }
        // hide ourselves
        ViewUtil.fadeOut(searchContainer);
        addRecentSearch(lastSearchedText);
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
            closeSearch();
            funnel.searchCancel();
            return true;
        }
        return false;
    }

    @Override
    public void setProgressBarEnabled(boolean enabled) {
        progressBar.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private void initSearchView() {
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setOnCloseListener(searchCloseListener);

        searchEditText = (EditText) searchView
                .findViewById(android.support.v7.appcompat.R.id.search_src_text);
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

        ImageView searchClose = (ImageView) searchView.findViewById(
                android.support.v7.appcompat.R.id.search_close_btn);
        FeedbackUtil.setToolbarButtonLongPressToast(searchClose);
    }

    private void initLangButton() {
        langButton.setText(app.getAppOrSystemLanguageCode().toUpperCase());
        formatLangButtonText();
        langButtonContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLangPreferenceDialog();
            }
        });
        FeedbackUtil.setToolbarButtonLongPressToast(langButtonContainer);
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
                navigateToTitle(firstResult, false, 0);
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
            closeSearch();
            funnel.searchCancel();
            return false;
        }
    };

    @Override
    public void navigateToTitle(@NonNull PageTitle title, boolean inNewTab, int position) {
        if (!isAdded()) {
            return;
        }
        funnel.searchClick(position);
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
        closeSearch();
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchSelectPage(historyEntry, inNewTab);
        }
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
                langButton.setText(langCode.substring(0, langButtonTextMaxLength).toUpperCase());
            }
            return;
        }
        langButton.setTextSize(langButtonTextSizeLarger);
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    public void showLangPreferenceDialog() {
        LanguagePreferenceDialog langPrefDialog = new LanguagePreferenceDialog(getContext(), true);
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
