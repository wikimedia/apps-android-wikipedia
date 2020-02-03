package org.wikipedia.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import androidx.fragment.app.Fragment;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.apache.commons.lang3.StringUtils.isBlank;
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
    private static final int MESSAGE_SEARCH = 1;
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

    private final LruCache<String, List<SearchResult>> searchResultsCache = new LruCache<>(MAX_CACHE_SIZE_SEARCH_RESULTS);
    private Handler searchHandler;
    private String currentSearchTerm = "";
    @Nullable private SearchResults lastFullTextResults;
    @NonNull private final List<SearchResult> totalResults = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);
        unbinder = ButterKnife.bind(this, view);

        SearchResultAdapter adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        searchErrorView.setBackClickListener((v) -> requireActivity().finish());
        searchErrorView.setRetryClickListener((v) -> {
            searchErrorView.setVisibility(View.GONE);
            startSearch(currentSearchTerm, true);
        });

        searchHandler = new Handler(new SearchHandlerCallback());

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
        searchResultsDisplay.setVisibility(View.VISIBLE);
    }

    public void hide() {
        searchResultsDisplay.setVisibility(View.GONE);
    }

    public boolean isShowing() {
        return searchResultsDisplay.getVisibility() == View.VISIBLE;
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
            return;
        }

        Message searchMessage = Message.obtain();
        searchMessage.what = MESSAGE_SEARCH;
        searchMessage.obj = term;

        if (force) {
            searchHandler.sendMessage(searchMessage);
        } else {
            searchHandler.sendMessageDelayed(searchMessage, DELAY_MILLIS);
        }
    }

    private class SearchHandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            if (!isAdded()) {
                return true;
            }
            final String mySearchTerm = (String) msg.obj;
            doTitlePrefixSearch(mySearchTerm);
            return true;
        }
    }

    private void doTitlePrefixSearch(final String searchTerm) {
        cancelSearchTask();
        final long startTime = System.nanoTime();
        updateProgressBar(true);

        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(getSearchLanguageCode())).prefixSearch(searchTerm, BATCH_SIZE, searchTerm)
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
                })
                .doAfterTerminate(() -> updateProgressBar(false))
                .subscribe(results -> {
                    searchErrorView.setVisibility(View.GONE);
                    handleResults(results, searchTerm, startTime);
                }, caught -> {
                    searchEmptyView.setVisibility(View.GONE);
                    searchErrorView.setVisibility(View.VISIBLE);
                    searchErrorView.setError(caught);
                    searchResultsContainer.setVisibility(View.GONE);
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
                    + String.format(getString(R.string.search_did_you_mean), suggestion)
                    + "</u>"));
            searchSuggestion.setTag(suggestion);
            searchSuggestion.setVisibility(View.VISIBLE);
        } else {
            searchSuggestion.setVisibility(View.GONE);
        }
    }

    private void cancelSearchTask() {
        updateProgressBar(false);
        searchHandler.removeMessages(MESSAGE_SEARCH);
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
                    searchErrorView.setVisibility(View.GONE);

                    // full text special:
                    SearchResultsFragment.this.lastFullTextResults = results;
                    displayResults(resultList);
                }, throwable -> {
                    // If there's an error, just log it and let the existing prefix search results be.
                    logError(true, startTime);
                }));
    }

    @Nullable
    public PageTitle getFirstResult() {
        if (!totalResults.isEmpty()) {
            return totalResults.get(0).getPageTitle();
        } else {
            return null;
        }
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
        searchResultsContainer.setVisibility(View.GONE);
        searchEmptyView.setVisibility(View.GONE);
        searchErrorView.setVisibility(View.GONE);
        if (clearSuggestion) {
            searchSuggestion.setVisibility(View.GONE);
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
                    break;
                }
            }
            if (!contains) {
                totalResults.add(newResult);
            }
        }

        if (totalResults.isEmpty()) {
            searchEmptyView.setVisibility(View.VISIBLE);
            searchResultsContainer.setVisibility(View.GONE);
        } else {
            searchEmptyView.setVisibility(View.GONE);
            searchResultsContainer.setVisibility(View.VISIBLE);
        }

        getAdapter().notifyDataSetChanged();
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
                convertView.setOnClickListener(this);
                convertView.setOnLongClickListener(this);
            }
            TextView pageTitleText = convertView.findViewById(R.id.page_list_item_title);
            SearchResult result = (SearchResult) getItem(position);

            SimpleDraweeView searchResultItemImage = convertView.findViewById(R.id.page_list_item_image);
            GoneIfEmptyTextView descriptionText = convertView.findViewById(R.id.page_list_item_description);
            TextView redirectText = convertView.findViewById(R.id.page_list_item_redirect);
            View redirectArrow = convertView.findViewById(R.id.page_list_item_redirect_arrow);
            if (TextUtils.isEmpty(result.getRedirectFrom())) {
                redirectText.setVisibility(View.GONE);
                redirectArrow.setVisibility(View.GONE);
                descriptionText.setText(result.getPageTitle().getDescription());
            } else {
                redirectText.setVisibility(View.VISIBLE);
                redirectArrow.setVisibility(View.VISIBLE);
                redirectText.setText(String.format(getString(R.string.search_redirect_from), result.getRedirectFrom()));
                descriptionText.setVisibility(View.GONE);
            }

            // highlight search term within the text
            StringUtil.boldenKeywordText(pageTitleText, result.getPageTitle().getDisplayText(), currentSearchTerm);

            searchResultItemImage.setVisibility((result.getPageTitle().getThumbUrl() == null) ? View.GONE : View.VISIBLE);
            ViewUtil.loadImageUrlInto(searchResultItemImage,
                    result.getPageTitle().getThumbUrl());

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == (totalResults.size() - 1) && WikipediaApp.getInstance().isOnline()) {
                if (lastFullTextResults == null) {
                    // the first full text search
                    doFullTextSearch(currentSearchTerm, null, false);
                } else if (lastFullTextResults.getContinuation() != null) {
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
}

