package org.wikipedia.search;

import org.wikipedia.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class SearchArticlesFragment extends Fragment {
    private static final String KEY_SEARCH_TERM = "searchTerm";
    private static final int DELAY_MILLIS = 300;
    private static final int MAX_CACHE_SIZE_SEARCH_RESULTS = 4;
    private static final int MAX_CACHE_SIZE_IMAGES = 48;
    private static final int MESSAGE_SEARCH = 1;
    private static final String ARG_LAST_SEARCHED_TEXT = "lastSearchedText";
    private static final String ARG_IS_SEARCH_ACTIVE = "isSearchActive";
    private static final String ARG_RESULTS_CACHE = "searchResultsCache";
    private static final String ARG_PAGE_IMAGES_CACHE = "pageImagesCache";

    private WikipediaApp app;
    private SearchResultAdapter adapter;
    private SearchView searchView;

    private ListView searchResultsList;
    private View searchNetworkError;
    private View searchNoResults;
    private View progressBar;
    private View fullSearchContainer;

    private boolean isSearchActive = false;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache
            = new ParcelableLruCache<List<PageTitle>>(MAX_CACHE_SIZE_SEARCH_RESULTS, List.class);
    private ParcelableLruCache<String> pageImagesCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_IMAGES, String.class);
    private String lastSearchedText;

    private Handler searchHandler;

    private SearchArticlesTask curSearchTask;
    private String searchTerm;

    /**
     * Factory method for creating new instances of the fragment.
     * @return new instance of this fragment.
     */
    public static SearchArticlesFragment newInstance(String searchTerm) {
        SearchArticlesFragment f = new SearchArticlesFragment();
        Bundle args = new Bundle();
        args.putString(KEY_SEARCH_TERM, searchTerm);
        f.setArguments(args);
        return f;
    }

    public SearchArticlesFragment() {
    }

    private void hideSearchResults() {
        searchResultsList.setVisibility(View.GONE);
        isSearchActive = false;
        searchHandler.removeMessages(MESSAGE_SEARCH);
        if (curSearchTask != null) {
            curSearchTask.cancel();
            curSearchTask = null;
        }
        progressBar.setVisibility(View.GONE);
        searchNetworkError.setVisibility(View.GONE);
        searchNoResults.setVisibility(View.GONE);
        fullSearchContainer.setVisibility(View.GONE);
    }

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private void displayResults(List<PageTitle> results) {
        adapter.setResults(results);

        if (app.getReleaseType() == WikipediaApp.RELEASE_PROD) {
            if (results.size() == 0) {
                searchNoResults.setVisibility(View.VISIBLE);
                searchResultsList.setVisibility(View.GONE);
            } else {
                searchNoResults.setVisibility(View.GONE);
                searchResultsList.setVisibility(View.VISIBLE);
            }
        } else {
            fullSearchContainer.setVisibility(View.VISIBLE);
            searchResultsList.setVisibility(View.VISIBLE);
        }

        //cache page thumbnails!
        for (PageTitle title : results) {
            if (title.getThumbUrl() == null) {
                continue;
            }
            pageImagesCache.put(title.getPrefixedText(), title.getThumbUrl());
        }
        ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        setHasOptionsMenu(true);
        searchTerm = getArguments().getString(KEY_SEARCH_TERM);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (WikipediaApp)getActivity().getApplicationContext();
        app.getBus().register(this);
        LinearLayout parentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_search, container, false);

        if (savedInstanceState != null) {
            getFromBundle(savedInstanceState);
        } else {
            getFromBundle(getArguments());
        }

        searchResultsList = (ListView) parentLayout.findViewById(R.id.search_results_list);
        searchNetworkError = parentLayout.findViewById(R.id.search_network_error);
        searchNoResults = parentLayout.findViewById(R.id.search_results_empty);
        progressBar = parentLayout.findViewById(R.id.search_progress);

        fullSearchContainer = parentLayout.findViewById(R.id.full_search_container);
        fullSearchContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideSoftKeyboard(getActivity());
                hideSearchResults();
                ((PageActivity)getActivity()).searchFullText(searchTerm);
            }
        });

        searchHandler = new Handler(new SearchHandlerCallback());

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) searchResultsList.getAdapter().getItem(position);
                navigateToTitle(title);
            }
        });

        searchNetworkError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch(searchTerm); //just retry!
                searchNetworkError.setVisibility(View.GONE);
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        return parentLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (isValidQuery(lastSearchedText)) {
            forceStartSearch(lastSearchedText);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((PageActivity)getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(true);
        saveToBundle(getArguments());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        app.getBus().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveToBundle(outState);
    }

    private void getFromBundle(Bundle inState) {
        ParcelableLruCache<List<PageTitle>> mySearchResultsCache = inState.getParcelable(ARG_RESULTS_CACHE);
        if (mySearchResultsCache != null) {
            searchResultsCache = mySearchResultsCache;
            pageImagesCache = inState.getParcelable(ARG_PAGE_IMAGES_CACHE);
            lastSearchedText = inState.getString(ARG_LAST_SEARCHED_TEXT);
            isSearchActive = inState.getBoolean(ARG_IS_SEARCH_ACTIVE);
        }
    }

    private void saveToBundle(Bundle outState) {
        outState.putString(ARG_LAST_SEARCHED_TEXT, lastSearchedText);
        outState.putBoolean(ARG_IS_SEARCH_ACTIVE, isSearchActive);
        outState.putParcelable(ARG_RESULTS_CACHE, searchResultsCache);
        outState.putParcelable(ARG_PAGE_IMAGES_CACHE, pageImagesCache);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((PageActivity)getActivity()).getDrawerToggle().setDrawerIndicatorEnabled(false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("");
    }

    @Subscribe
    public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
        updateZeroChrome();
    }

    public void newSearch(String term) {
        //update this fragment's arguments...
        getArguments().putString(KEY_SEARCH_TERM, term);
        searchTerm = term;
        //if we're already attached, then do the search...
        if (isAdded()) {
            startSearch(term);
        }
    }

    private void startSearch(String term) {
        if (Utils.compareStrings(term, lastSearchedText) && isSearchActive) {
            return; // Nothing has changed!
        }
        forceStartSearch(term);
    }

    private void forceStartSearch(String term) {
        if (term.equals("")) {
            hideSearchResults();
            fullSearchContainer.setVisibility(View.GONE);
            return;
        }

        searchNoResults.setVisibility(View.GONE);
        searchNetworkError.setVisibility(View.GONE);

        if (app.getReleaseType() != WikipediaApp.RELEASE_PROD) {
            fullSearchContainer.setVisibility(View.VISIBLE);
        }

        List<PageTitle> cacheResult = searchResultsCache.get(app.getPrimaryLanguage() + "-" + term);
        if (cacheResult != null) {
            displayResults(cacheResult);
            isSearchActive = true;
            return;
        }
        searchHandler.removeMessages(MESSAGE_SEARCH);
        Message searchMessage = Message.obtain();
        searchMessage.what = MESSAGE_SEARCH;
        searchMessage.obj = term;

        searchHandler.sendMessageDelayed(searchMessage, DELAY_MILLIS);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        addSearchView(menu);
    }

    private void addSearchView(Menu menu) {
        MenuItem searchAction = menu.add(0, Menu.NONE, Menu.NONE, getString(R.string.search_hint));
        MenuItemCompat.setShowAsAction(searchAction, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        searchView = new SearchView(getActivity());
        searchView.setFocusable(true);
        searchView.requestFocusFromTouch();
        searchView.setOnQueryTextListener(searchQueryListener);
        searchView.setOnCloseListener(searchCloseListener);
        searchView.setIconified(false);
        searchView.setInputType(EditorInfo.TYPE_CLASS_TEXT);
        searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
        searchView.setSubmitButtonEnabled(true);
        updateZeroChrome();
        if (isValidQuery(lastSearchedText)) {
            searchView.setQuery(lastSearchedText, false);
        }
        MenuItemCompat.setActionView(searchAction, searchView);
    }

    /*
    Update any UI elements related to WP Zero
     */
    private void updateZeroChrome() {
        if (searchView != null) {
            searchView.setQueryHint(
                    app.getWikipediaZeroHandler().isZeroEnabled() ? getString(R.string.zero_search_hint) : getString(R.string.search_hint));
        }
    }

    private boolean isValidQuery(String queryText) {
        return queryText != null && TextUtils.getTrimmedLength(queryText) > 0;
    }

    private final SearchView.OnQueryTextListener searchQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String queryText) {
            searchTerm = queryText;
            if (isValidQuery(queryText)) {
                navigateToTitle(queryText);
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String queryText) {
            searchTerm = queryText;
            if (isValidQuery(queryText)) {
                startSearch(queryText);
            }
            return true;
        }
    };

    private final SearchView.OnCloseListener searchCloseListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            closeSearch();
            ((PageActivity)getActivity()).closeSearch();
            return false;
        }
    };

    private void closeSearch() {
        hideSearchResults();
    }

    private void navigateToTitle(String queryText) {
        navigateToTitle(new PageTitle(queryText, app.getPrimarySite(), null));
    }

    public void navigateToTitle(PageTitle title) {
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
        Utils.hideSoftKeyboard(getActivity());
        app.getBus().post(new NewWikiPageNavigationEvent(title, historyEntry));
    }

    private final class SearchResultAdapter extends BaseAdapter {
        private List<PageTitle> results;
        private final LayoutInflater inflater;

        private SearchResultAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        private void setResults(List<PageTitle> results) {
            this.results = results;
        }

        @Override
        public int getCount() {
            return results == null ? 0 : results.size();
        }

        @Override
        public Object getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_search_result, parent, false);
            }
            TextView pageTitleText = (TextView) convertView.findViewById(R.id.result_text);
            PageTitle title = (PageTitle) getItem(position);
            pageTitleText.setText(title.getDisplayText());
            ImageView imageView = (ImageView) convertView.findViewById(R.id.result_image);

            String thumbnail = pageImagesCache.get(title.getPrefixedText());
            if (thumbnail == null) {
                Picasso.with(getActivity())
                        .load(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            } else {
                Picasso.with(getActivity())
                        .load(thumbnail)
                        .placeholder(R.drawable.ic_pageimage_placeholder)
                        .error(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            }

            return convertView;
        }
    }

    private class SearchHandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            final String mySearchTerm = (String) msg.obj;
            SearchArticlesTask searchTask = new SearchArticlesTask(app, app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), mySearchTerm) {
                @Override
                public void onBeforeExecute() {
                    progressBar.setVisibility(View.VISIBLE);
                    isSearchActive = true;
                }

                @Override
                public void onFinish(List<PageTitle> result) {
                    progressBar.setVisibility(View.GONE);
                    searchNetworkError.setVisibility(View.GONE);
                    displayResults(result);
                    searchResultsCache.put(app.getPrimaryLanguage() + "-" + mySearchTerm, result);
                    lastSearchedText = mySearchTerm;
                    curSearchTask = null;
                }

                @Override
                public void onCatch(Throwable caught) {
                    progressBar.setVisibility(View.GONE);
                    searchNetworkError.setVisibility(View.VISIBLE);
                    searchResultsList.setVisibility(View.GONE);
                    curSearchTask = null;
                }
            };
            if (curSearchTask != null) {
                // This does not cancel the HTTP request itself
                // But it does cancel th execution of onFinish
                // This makes sure that a slower previous search query does not override
                // the results of a newer search query
                curSearchTask.cancel();
            }
            curSearchTask = searchTask;
            searchTask.execute();
            return true;
        }
    }
}
