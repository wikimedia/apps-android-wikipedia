package org.wikipedia.search;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.collection.LruCache;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.TabCountsView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.wikipedia.Constants.HISTORY_FRAGMENT_LOADER_ID;
import static org.wikipedia.Constants.SEARCH_FRAGMENT_HISTORY_LOADER_ID;
import static org.wikipedia.search.SearchResult.SearchResultTypeWithPriority.HISTORY_SEARCH_RESULT;
import static org.wikipedia.search.SearchResult.SearchResultTypeWithPriority.READING_LIST_SEARCH_RESULT;
import static org.wikipedia.search.SearchResult.SearchResultTypeWithPriority.SEARCH_RESULT;
import static org.wikipedia.search.SearchResult.SearchResultTypeWithPriority.TAB_LIST_SEARCH_RESULT;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class SearchResultsFragment extends Fragment {
    public interface Callback {
        void onSearchResultCopyLink(@NonNull PageTitle title);
        void onSearchResultAddToList(@NonNull PageTitle title, @NonNull InvokeSource source);
        void onSearchResultShareLink(@NonNull PageTitle title);
        void onSearchProgressBar(boolean enabled);
        void navigateToTitle(@NonNull PageTitle item, boolean inNewTab, int position);
        void setSearchText(@NonNull CharSequence text);
        @NonNull SearchFunnel getFunnel();
    }

    private static final int BATCH_SIZE = 20;
    private static final int DELAY_MILLIS = 300;
    private static final int MAX_CACHE_SIZE_SEARCH_RESULTS = 4;
    /**
     * Constant to ease in the conversion of timestamps from nanoseconds to milliseconds.
     */
    private static final int NANO_TO_MILLI = 1_000_000;

    @BindView(R.id.search_results_display) View searchResultsDisplay;
    @BindView(R.id.search_results_container) View searchResultsContainer;
    @BindView(R.id.search_results_list) ListView searchResultsList;
    @BindView(R.id.search_error_view) WikiErrorView searchErrorView;
    @BindView(R.id.search_empty_view) View searchEmptyView;
    @BindView(R.id.search_suggestion) TextView searchSuggestion;
    private Unbinder unbinder;
    private Loader<Cursor> loaderManager;

    private final LruCache<String, List<SearchResult>> searchResultsCache = new LruCache<>(MAX_CACHE_SIZE_SEARCH_RESULTS);
    private String currentSearchTerm = "";
    @Nullable private SearchResults lastFullTextResults;
    @NonNull private final List<SearchResult> totalResults = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();
    private LoaderCallback loaderCallback = new LoaderCallback();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);
        unbinder = ButterKnife.bind(this, view);

        SearchResultAdapter adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        searchErrorView.setBackClickListener((v) -> requireActivity().finish());
        searchErrorView.setRetryClickListener((v) -> {
            searchErrorView.setVisibility(GONE);
            startSearch(currentSearchTerm, true);
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        new LongPressHandler(searchResultsList, HistoryEntry.SOURCE_SEARCH,
                new SearchResultsFragmentLongPressHandler());
    }

    @Override
    public void onDestroyView() {
        LoaderManager.getInstance(requireActivity()).destroyLoader(HISTORY_FRAGMENT_LOADER_ID);
        searchErrorView.setRetryClickListener(null);
        unbinder.unbind();
        unbinder = null;
        disposables.clear();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R.id.search_suggestion) void onSuggestionClick(View view) {
        Callback callback = callback();
        String suggestion = (String) searchSuggestion.getTag();
        if (callback != null && suggestion != null) {
            callback.getFunnel().searchDidYouMean(getSearchLanguageCode());
            callback.setSearchText(suggestion);
            startSearch(suggestion, true);
        }
    }

    public void show() {
        searchResultsDisplay.setVisibility(VISIBLE);
    }

    public void hide() {
        searchResultsDisplay.setVisibility(GONE);
    }

    public boolean isShowing() {
        return searchResultsDisplay.getVisibility() == VISIBLE;
    }

    public void setLayoutDirection(@NonNull String langCode) {
        setConditionalLayoutDirection(searchResultsList, langCode);
    }

    /**
     * Kick off a search, based on a given search term.
     * @param term Phrase to search for.
     * @param force Whether to "force" starting this search. If the search is not forced, the
     *              search may be delayed by a small time, so that network requests are not sent
     *              too often.  If the search is forced, the network request is sent immediately.
     */
    public void startSearch(@Nullable String term, boolean force) {
        if (!force && StringUtils.equals(currentSearchTerm, term)) {
            return;
        }

        cancelSearchTask();
        currentSearchTerm = term;

        if (isBlank(term)) {
            clearResults();
            return;
        }

        List<SearchResult> cacheResult = searchResultsCache.get(getSearchLanguageCode() + "-" + term);
        if (cacheResult != null && !cacheResult.isEmpty()) {
            clearResults();
            displayResults(cacheResult);
            gatherSearchResultsFromAppSources();
            return;
        }

        doTitlePrefixSearch(term, force);
    }

    private void doTitlePrefixSearch(final String searchTerm, boolean force) {
        cancelSearchTask();
        final long startTime = System.nanoTime();
        updateProgressBar(true);

        disposables.add(Observable.timer(force ? 0 : DELAY_MILLIS, TimeUnit.MILLISECONDS).flatMap(timer ->
                ServiceFactory.get(WikiSite.forLanguageCode(getSearchLanguageCode())).prefixSearch(searchTerm, BATCH_SIZE, searchTerm)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(response -> {
                            if (response != null && response.query() != null && response.query().pages() != null) {
                                // noinspection ConstantConditions
                                return new SearchResults(response.query().pages(),
                                        WikiSite.forLanguageCode(getSearchLanguageCode()), response.continuation(),
                                        response.suggestion());
                            }
                            // A prefix search query with no results will return the following:
                            //
                            // {
                            //   "batchcomplete": true,
                            //   "query": {
                            //      "search": []
                            //   }
                            // }
                            //
                            // Just return an empty SearchResults() in this case.
                            return new SearchResults();
                        }))
                .doAfterTerminate(() -> updateProgressBar(false))
                .subscribe(results -> {
                    searchErrorView.setVisibility(GONE);
                    handleResults(results, searchTerm, startTime);
                }, caught -> {
                    searchEmptyView.setVisibility(GONE);
                    searchErrorView.setVisibility(VISIBLE);
                    searchErrorView.setError(caught);
                    searchResultsContainer.setVisibility(GONE);
                    logError(false, startTime);
                }));
    }

    private void handleResults(@NonNull SearchResults results, @NonNull String searchTerm, long startTime) {
        List<SearchResult> resultList = results.getResults();
        // To ease data analysis and better make the funnel track with user behaviour,
        // only transmit search results events if there are a nonzero number of results
        if (!resultList.isEmpty()) {
            clearResults();
            displayResults(resultList);
            gatherSearchResultsFromAppSources();
            log(resultList, startTime);
        }

        handleSuggestion(results.getSuggestion());

        // add titles to cache...
        searchResultsCache.put(getSearchLanguageCode() + "-" + searchTerm, resultList);

        // scroll to top, but post it to the message queue, because it should be done
        // after the data set is updated.
        searchResultsList.post(() -> {
            if (!isAdded()) {
                return;
            }
            searchResultsList.setSelectionAfterHeaderView();
        });

        if (resultList.isEmpty()) {
            // kick off full text search if we get no results
            doFullTextSearch(currentSearchTerm, null, true);
        }
    }

    private void handleSuggestion(@Nullable String suggestion) {
        if (suggestion != null) {
            searchSuggestion.setText(StringUtil.fromHtml("<u>"
                    + getString(R.string.search_did_you_mean, suggestion)
                    + "</u>"));
            searchSuggestion.setTag(suggestion);
            searchSuggestion.setVisibility(VISIBLE);
        } else {
            searchSuggestion.setVisibility(GONE);
        }
    }

    private void cancelSearchTask() {
        updateProgressBar(false);
        disposables.clear();
    }

    private void doFullTextSearch(final String searchTerm,
                                  final Map<String, String> continueOffset,
                                  final boolean clearOnSuccess) {
        final long startTime = System.nanoTime();
        updateProgressBar(true);

        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(getSearchLanguageCode())).fullTextSearch(searchTerm, BATCH_SIZE,
                continueOffset != null ? continueOffset.get("continue") : null, continueOffset != null ? continueOffset.get("gsroffset") : null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> {
                    if (response.query() != null) {
                        // noinspection ConstantConditions
                        return new SearchResults(response.query().pages(), WikiSite.forLanguageCode(getSearchLanguageCode()),
                                response.continuation(), null);
                    }
                    // A 'morelike' search query with no results will just return an API warning:
                    //
                    // {
                    //   "batchcomplete": true,
                    //   "warnings": {
                    //      "search": {
                    //        "warnings": "No valid titles provided to 'morelike'."
                    //      }
                    //   }
                    // }
                    //
                    // Just return an empty SearchResults() in this case.
                    return new SearchResults();
                })
                .doAfterTerminate(() -> updateProgressBar(false))
                .subscribe(results -> {
                    List<SearchResult> resultList = results.getResults();
                    cache(resultList, searchTerm);
                    log(resultList, startTime);
                    if (clearOnSuccess) {
                        clearResults(false);
                    }
                    searchErrorView.setVisibility(GONE);

                    // full text special:
                    SearchResultsFragment.this.lastFullTextResults = results;
                    displayResults(resultList);
                    gatherSearchResultsFromAppSources();
                }, throwable -> {
                    // If there's an error, just log it and let the existing prefix search results be.
                    logError(true, startTime);
                }));
    }

    private void clearResults() {
        clearResults(true);
    }

    private void updateProgressBar(boolean enabled) {
        Callback callback = callback();
        if (callback != null) {
            callback.onSearchProgressBar(enabled);
        }
    }

    private void clearResults(boolean clearSuggestion) {
        searchResultsContainer.setVisibility(GONE);
        searchEmptyView.setVisibility(GONE);
        searchErrorView.setVisibility(GONE);
        if (clearSuggestion) {
            searchSuggestion.setVisibility(GONE);
        }

        lastFullTextResults = null;

        totalResults.clear();

        getAdapter().notifyDataSetChanged();
    }

    private BaseAdapter getAdapter() {
        return (BaseAdapter) searchResultsList.getAdapter();
    }

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private void displayResults(List<SearchResult> results) {
        for (SearchResult newResult : results) {
            boolean contains = false;
            for (SearchResult result : totalResults) {
                if (newResult.getPageTitle().equals(result.getPageTitle())) {
                    contains = true;
                    if (newResult.getPriority() == SEARCH_RESULT.getPriority()
                            || newResult.getPriority() <= result.getPriority()) {
                        break;
                    }
                    totalResults.remove(result);
                    insertSearchResultInCorrectPosition(newResult);
                    break;
                }
            }
            if (!contains) {
                if (newResult.getPriority() == SEARCH_RESULT.getPriority() || totalResults.isEmpty()) {
                    totalResults.add(newResult);
                } else {
                    insertSearchResultInCorrectPosition(newResult);
                }
            }
        }

        if (totalResults.isEmpty()) {
            searchEmptyView.setVisibility(VISIBLE);
            searchResultsContainer.setVisibility(GONE);
        } else {
            searchEmptyView.setVisibility(GONE);
            searchResultsContainer.setVisibility(VISIBLE);
        }

        getAdapter().notifyDataSetChanged();
    }

    private void insertSearchResultInCorrectPosition(@NonNull SearchResult newResult) {
        final int numOfResultsFromAppSources = 3;

        for (ListIterator<SearchResult> iterator = totalResults.listIterator(); totalResults.size() >= iterator.nextIndex() && iterator.nextIndex() < numOfResultsFromAppSources;) {
            int currentPos = iterator.nextIndex();
            SearchResult resultInPosition = iterator.next();
            if (resultInPosition.getPriority() == newResult.getPriority() && !resultInPosition.getPageTitle().getText().equals(newResult.getPageTitle().getText())) {
                //replace search result
                iterator.remove();
                iterator.add(newResult);
                return;
            }
            if (resultInPosition.getPriority() < newResult.getPriority()) {
                totalResults.add(currentPos, newResult);
                return;
            }
        }
        //Results list was shorter than 3, so add to the end
        totalResults.add(newResult);
    }

    private class SearchResultsFragmentLongPressHandler
            implements org.wikipedia.LongPressHandler.ListViewOverflowMenuListener {
        private int lastPositionRequested;

        @Override
        public PageTitle getTitleForListPosition(int position) {
            lastPositionRequested = position;
            return ((SearchResult) getAdapter().getItem(position)).getPageTitle();
        }

        @Override
        public void onOpenLink(PageTitle title, HistoryEntry entry) {
            Callback callback = callback();
            if (callback != null) {
                callback.navigateToTitle(title, false, lastPositionRequested);
            }
        }

        @Override
        public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
            Callback callback = callback();
            if (callback != null) {
                callback.navigateToTitle(title, true, lastPositionRequested);
            }
        }

        @Override
        public void onCopyLink(PageTitle title) {
            Callback callback = callback();
            if (callback != null) {
                callback.onSearchResultCopyLink(title);
            }
        }

        @Override
        public void onShareLink(PageTitle title) {
            Callback callback = callback();
            if (callback != null) {
                callback.onSearchResultShareLink(title);
            }
        }

        @Override
        public void onAddToList(@NonNull PageTitle title, @NonNull InvokeSource source) {
            Callback callback = callback();
            if (callback != null) {
                callback.onSearchResultAddToList(title, source);
            }
        }
    }

    private final class SearchResultAdapter extends BaseAdapter implements View.OnClickListener, View.OnLongClickListener {
        private final LayoutInflater inflater;

        SearchResultAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public int getCount() {
            return totalResults.size();
        }

        @Override
        public Object getItem(int position) {
            return totalResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_search_result, parent, false);
                convertView.setFocusable(true);
                convertView.setOnClickListener(this);
                convertView.setOnLongClickListener(this);
            }
            TextView pageTitleText = convertView.findViewById(R.id.page_list_item_title);
            SearchResult result = (SearchResult) getItem(position);
            int resultPriority = result.getPriority();

            ImageView searchResultItemImage = convertView.findViewById(R.id.page_list_item_image);
            ImageView searchResultIcon = convertView.findViewById(R.id.page_list_icon);
            TabCountsView searchResultTabCountsView = convertView.findViewById(R.id.page_list_button_tabs);
            searchResultTabCountsView.updateTabCount();
            GoneIfEmptyTextView descriptionText = convertView.findViewById(R.id.page_list_item_description);
            TextView redirectText = convertView.findViewById(R.id.page_list_item_redirect);
            View redirectArrow = convertView.findViewById(R.id.page_list_item_redirect_arrow);
            if (TextUtils.isEmpty(result.getRedirectFrom())) {
                redirectText.setVisibility(GONE);
                redirectArrow.setVisibility(GONE);
                descriptionText.setText(result.getPageTitle().getDescription());
            } else {
                redirectText.setVisibility(VISIBLE);
                redirectArrow.setVisibility(VISIBLE);
                redirectText.setText(getString(R.string.search_redirect_from, result.getRedirectFrom()));
                descriptionText.setVisibility(GONE);
            }
            if (resultPriority == SEARCH_RESULT.getPriority()) {
                searchResultIcon.setVisibility(GONE);
                searchResultTabCountsView.setVisibility(GONE);
            } else {
                searchResultTabCountsView.setVisibility(resultPriority == TAB_LIST_SEARCH_RESULT.getPriority() ? VISIBLE : GONE);
                searchResultIcon.setVisibility(resultPriority == TAB_LIST_SEARCH_RESULT.getPriority() ? GONE : VISIBLE);
                searchResultIcon.setImageDrawable(AppCompatResources.getDrawable(requireContext(),
                        resultPriority == HISTORY_SEARCH_RESULT.getPriority() ? R.drawable.ic_baseline_history_24 : R.drawable.ic_bookmark_border_white_24dp));
            }

            // highlight search term within the text
            StringUtil.boldenKeywordText(pageTitleText, result.getPageTitle().getDisplayText(), currentSearchTerm);

            searchResultItemImage.setVisibility((result.getPageTitle().getThumbUrl() == null) ? GONE : VISIBLE);
            ViewUtil.loadImageWithRoundedCorners(searchResultItemImage, result.getPageTitle().getThumbUrl());

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == (totalResults.size() - 1) && WikipediaApp.getInstance().isOnline()) {
                if (lastFullTextResults == null) {
                    // the first full text search
                    doFullTextSearch(currentSearchTerm, null, false);
                } else if (lastFullTextResults.getContinuation() != null && !lastFullTextResults.getContinuation().isEmpty()) {
                    // subsequent full text searches
                    doFullTextSearch(currentSearchTerm, lastFullTextResults.getContinuation(), false);
                }
            }

            convertView.setTag(position);
            return convertView;
        }

        @Override
        public void onClick(View v) {
            Callback callback = callback();
            int position = (int) v.getTag();
            if (callback != null && position < totalResults.size()) {
                callback.navigateToTitle(totalResults.get(position).getPageTitle(), false, position);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }
    }

    private void cache(@NonNull List<SearchResult> resultList, @NonNull String searchTerm) {
        String cacheKey = getSearchLanguageCode() + "-" + searchTerm;
        List<SearchResult> cachedTitles = searchResultsCache.get(cacheKey);
        if (cachedTitles != null) {
            cachedTitles.addAll(resultList);
            searchResultsCache.put(cacheKey, cachedTitles);
        }
    }

    private void log(@NonNull List<SearchResult> resultList, long startTime) {
        // To ease data analysis and better make the funnel track with user behaviour,
        // only transmit search results events if there are a nonzero number of results
        if (callback() != null && !resultList.isEmpty()) {
            // noinspection ConstantConditions
            callback().getFunnel().searchResults(true, resultList.size(), displayTime(startTime), getSearchLanguageCode());
        }
    }

    private void logError(boolean fullText, long startTime) {
        if (callback() != null) {
            // noinspection ConstantConditions
            callback().getFunnel().searchError(fullText, displayTime(startTime), getSearchLanguageCode());
        }
    }

    private int displayTime(long startTime) {
        return (int) ((System.nanoTime() - startTime) / NANO_TO_MILLI);
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private String getSearchLanguageCode() {
        return ((SearchFragment) getParentFragment()).getSearchLanguageCode();
    }

    private void gatherSearchResultsFromAppSources() {
        addSearchResultFromReadingLists();
        addSearchResultFromOpenTabs();
        addSearchResultFromHistoryEntries();
    }

    private void addSearchResultFromHistoryEntries() {
        if (loaderManager == null) {
            loaderManager = LoaderManager.getInstance(requireActivity()).initLoader(SEARCH_FRAGMENT_HISTORY_LOADER_ID, null, loaderCallback);
        } else {
            LoaderManager.getInstance(requireActivity()).restartLoader(SEARCH_FRAGMENT_HISTORY_LOADER_ID, null, loaderCallback);
        }
    }

    private void addSearchResultFromReadingLists() {
        List<SearchResult> searchResultsFromAppSources = new ArrayList<>();
        List<Object> list = ReadingListBehaviorsUtil.INSTANCE.applySearchQuery(currentSearchTerm, ReadingListDbHelper.instance().getAllLists());
        for (Object o : list) {
            if (o instanceof ReadingListPage) {
                ReadingListPage page = (ReadingListPage) o;
                PageTitle pageTitle = new PageTitle(page.title(), page.wiki(), page.thumbUrl());
                SearchResult searchResult = new SearchResult(pageTitle);
                searchResult.setSearchResultTypeWithPriority(READING_LIST_SEARCH_RESULT);
                searchResultsFromAppSources.add(searchResult);
                displayResults(searchResultsFromAppSources);
                break;
            }
        }
    }

    private void addSearchResultFromOpenTabs() {
        List<SearchResult> searchResultsFromAppSources = new ArrayList<>();
        List<Tab> tabList = WikipediaApp.getInstance().getTabList();
        for (Tab tab : tabList) {
            if (tab.getBackStackPositionTitle() != null && tab.getBackStackPositionTitle().getDisplayText().toLowerCase().contains(currentSearchTerm.toLowerCase())) {
                SearchResult searchResult = new SearchResult(tab.getBackStackPositionTitle());
                searchResult.setSearchResultTypeWithPriority(TAB_LIST_SEARCH_RESULT);
                searchResultsFromAppSources.add(searchResult);
                displayResults(searchResultsFromAppSources);
                return;
            }
        }
    }

    private class LoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        @NonNull @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String titleCol = PageHistoryContract.PageWithImage.API_TITLE.qualifiedName();
            String selection = null;
            String[] selectionArgs = null;
            String searchStr = currentSearchTerm;
            if (!TextUtils.isEmpty(searchStr)) {
                searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                selection = "UPPER(" + titleCol + ") LIKE UPPER(?) ESCAPE '\\'";
                selectionArgs = new String[]{"%" + searchStr + "%"};
            }
            Uri uri = PageHistoryContract.PageWithImage.URI;
            String order = PageHistoryContract.PageWithImage.ORDER_MRU;
            return new CursorLoader(requireContext().getApplicationContext(), uri, null, selection, selectionArgs, order);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
            HistoryFragment.IndexedHistoryEntry indexedEntry = null;
            if (cursor.getCount() > 1) {
                cursor.moveToFirst();
                indexedEntry = new HistoryFragment.IndexedHistoryEntry(cursor);
                List<SearchResult> searchResults = new ArrayList<>();
                PageTitle pageTitle = indexedEntry.getEntry().getTitle();
                pageTitle.setThumbUrl(indexedEntry.getImageUrl());
                SearchResult searchResult = new SearchResult(pageTitle);
                searchResult.setSearchResultTypeWithPriority(HISTORY_SEARCH_RESULT);
                searchResults.add(searchResult);
                displayResults(searchResults);
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            loader.cancelLoad();
        }
    }
}

