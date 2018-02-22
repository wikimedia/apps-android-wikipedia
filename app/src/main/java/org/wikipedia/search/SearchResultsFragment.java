package org.wikipedia.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.SearchFunnel;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import butterknife.Unbinder;
import retrofit2.Call;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class SearchResultsFragment extends Fragment {
    public interface Callback {
        void onSearchResultCopyLink(@NonNull PageTitle title);
        void onSearchResultAddToList(@NonNull PageTitle title,
                                     @NonNull AddToReadingListDialog.InvokeSource source);
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

    private WikipediaApp app;
    private final LruCache<String, List<SearchResult>> searchResultsCache = new LruCache<>(MAX_CACHE_SIZE_SEARCH_RESULTS);
    private Handler searchHandler;
    private String currentSearchTerm = "";
    @Nullable private SearchResults lastFullTextResults;
    @NonNull private final List<SearchResult> totalResults = new ArrayList<>();
    private PrefixSearchClient prefixSearchClient = new PrefixSearchClient();
    private FullTextSearchClient fullTextSearchClient = new FullTextSearchClient();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_results, container, false);
        unbinder = ButterKnife.bind(this, view);

        SearchResultAdapter adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

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
        super.onDestroyView();
    }

    @OnItemClick(R.id.search_results_list) void onItemClick(ListView view, int position) {
        Callback callback = callback();
        if (callback != null) {
            PageTitle item = ((SearchResult) getAdapter().getItem(position)).getPageTitle();
            callback.navigateToTitle(item, false, position);
        }
    }

    @OnClick(R.id.search_suggestion) void onSuggestionClick(View view) {
        Callback callback = callback();
        String suggestion = (String) searchSuggestion.getTag();
        if (callback != null && suggestion != null) {
            callback.getFunnel().searchDidYouMean();
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

    /**
     * Kick off a search, based on a given search term.
     * @param term Phrase to search for.
     * @param force Whether to "force" starting this search. If the search is not forced, the
     *              search may be delayed by a small time, so that network requests are not sent
     *              too often.  If the search is forced, the network request is sent immediately.
     */
    public void startSearch(@Nullable String term, boolean force) {
        if (!force && currentSearchTerm.equals(term)) {
            return;
        }

        cancelSearchTask();
        currentSearchTerm = term;

        if (isBlank(term)) {
            clearResults();
            return;
        }

        List<SearchResult> cacheResult = searchResultsCache.get(app.getAppOrSystemLanguageCode() + "-" + term);
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
            if (OfflineManager.hasCompilation() && !DeviceUtil.isOnline()) {
                doOfflineSearch(mySearchTerm);
            } else {
                doTitlePrefixSearch(mySearchTerm);
            }
            return true;
        }
    }

    private void doOfflineSearch(final String searchTerm) {
        searchSuggestion.setVisibility(View.GONE);
        searchErrorView.setVisibility(View.GONE);
        updateProgressBar(false);

        List<SearchResult> resultList = new ArrayList<>();
        try {
            List<String> results = OfflineManager.instance().searchByPrefix(searchTerm, BATCH_SIZE);
            for (String title : results) {
                resultList.add(new SearchResult(new PageTitle(title, app.getWikiSite())));
            }
        } catch (IOException e) {
            L.d(e);
        }

        clearResults();
        displayResults(resultList);
    }

    private void doTitlePrefixSearch(final String searchTerm) {
        cancelSearchTask();
        final long startTime = System.nanoTime();
        updateProgressBar(true);

        prefixSearchClient.request(app.getWikiSite(), searchTerm, new PrefixSearchClient.Callback() {
            @Override
            public void success(@NonNull Call<PrefixSearchResponse> call, @NonNull SearchResults results) {
                if (!isAdded()) {
                    return;
                }
                updateProgressBar(false);
                searchErrorView.setVisibility(View.GONE);
                handleResults(results, searchTerm, startTime);
            }

            @Override
            public void failure(@NonNull Call<PrefixSearchResponse> call, @NonNull Throwable caught) {
                if (callCanceledIoException(caught)) {
                    return;
                }
                if (isAdded()) {
                    updateProgressBar(false);
                    searchEmptyView.setVisibility(View.GONE);
                    searchErrorView.setVisibility(View.VISIBLE);
                    searchErrorView.setError(caught);
                    searchResultsContainer.setVisibility(View.GONE);
                }
                logError(false, startTime);
            }
        });
    }

    /* Catch and discard exceptions thrown when our Retrofit calls are (intentionally) canceled. */
    private boolean callCanceledIoException(@NonNull Throwable caught) {
        return caught instanceof IOException && "Canceled".equals(caught.getMessage());
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
        searchResultsCache.put(app.getAppOrSystemLanguageCode() + "-" + searchTerm, resultList);

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
        prefixSearchClient.cancel();
        fullTextSearchClient.cancel();
    }

    private void doFullTextSearch(final String searchTerm,
                                  final Map<String, String> continueOffset,
                                  final boolean clearOnSuccess) {
        final long startTime = System.nanoTime();
        updateProgressBar(true);

        fullTextSearchClient.request(
                app.getWikiSite(),
                searchTerm,
                continueOffset != null ? continueOffset.get("continue") : null,
                continueOffset != null ? continueOffset.get("gsroffset") : null,
                BATCH_SIZE,
                new FullTextSearchCallback(searchTerm, startTime, clearOnSuccess));
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
            implements org.wikipedia.LongPressHandler.ListViewContextMenuListener {
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
        public void onAddToList(@NonNull PageTitle title,
                                @NonNull AddToReadingListDialog.InvokeSource source) {
            Callback callback = callback();
            if (callback != null) {
                callback.onSearchResultAddToList(title, source);
            }
        }
    }

    private final class SearchResultAdapter extends BaseAdapter {
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
            }
            TextView pageTitleText = convertView.findViewById(R.id.page_list_item_title);
            SearchResult result = (SearchResult) getItem(position);

            GoneIfEmptyTextView descriptionText = convertView.findViewById(R.id.page_list_item_description);
            View redirectContainer = convertView.findViewById(R.id.page_list_item_redirect_container);
            if (TextUtils.isEmpty(result.getRedirectFrom())) {
                redirectContainer.setVisibility(View.GONE);
                descriptionText.setText(StringUtils.capitalize(result.getPageTitle().getDescription()));
            } else {
                redirectContainer.setVisibility(View.VISIBLE);
                descriptionText.setVisibility(View.GONE);
                TextView redirectText = convertView.findViewById(R.id.page_list_item_redirect);
                redirectText.setText(String.format(getString(R.string.search_redirect_from), result.getRedirectFrom()));
            }

            // highlight search term within the text
            String displayText = result.getPageTitle().getDisplayText();
            int startIndex = indexOf(displayText, currentSearchTerm);
            if (startIndex >= 0) {
                displayText = displayText.substring(0, startIndex)
                      + "<strong>"
                      + displayText.substring(startIndex, startIndex + currentSearchTerm.length())
                      + "</strong>"
                      + displayText.substring(startIndex + currentSearchTerm.length(),
                                              displayText.length());
                pageTitleText.setText(StringUtil.fromHtml(displayText));
            } else {
                pageTitleText.setText(displayText);
            }

            ViewUtil.loadImageUrlInto((SimpleDraweeView) convertView.findViewById(R.id.page_list_item_image),
                    result.getPageTitle().getThumbUrl());

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == (totalResults.size() - 1) && DeviceUtil.isOnline()) {
                if (lastFullTextResults == null) {
                    // the first full text search
                    doFullTextSearch(currentSearchTerm, null, false);
                } else if (lastFullTextResults.getContinuation() != null) {
                    // subsequent full text searches
                    doFullTextSearch(currentSearchTerm, lastFullTextResults.getContinuation(), false);
                }
            }

            return convertView;
        }

        // case insensitive indexOf, also more lenient with similar chars, like chars with accents
        private int indexOf(String original, String search) {
            Collator collator = Collator.getInstance();
            collator.setStrength(Collator.PRIMARY);
            for (int i = 0; i <= original.length() - search.length(); i++) {
                if (collator.equals(search, original.substring(i, i + search.length()))) {
                    return i;
                }
            }
            return -1;
        }
    }

    private void cache(@NonNull List<SearchResult> resultList, @NonNull String searchTerm) {
        String cacheKey = app.getAppOrSystemLanguageCode() + "-" + searchTerm;
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
            callback().getFunnel().searchResults(true, resultList.size(), displayTime(startTime));
        }
    }

    private void logError(boolean fullText, long startTime) {
        if (callback() != null) {
            // noinspection ConstantConditions
            callback().getFunnel().searchError(fullText, displayTime(startTime));
        }
    }

    private int displayTime(long startTime) {
        return (int) ((System.nanoTime() - startTime) / NANO_TO_MILLI);
    }

    private final class FullTextSearchCallback implements FullTextSearchClient.Callback {
        @NonNull private String searchTerm = "";
        private long startTime;
        private boolean clearOnSuccess;

        private FullTextSearchCallback(@NonNull String searchTerm, long startTime, boolean clearOnSuccess) {
            this.searchTerm = searchTerm;
            this.startTime = startTime;
            this.clearOnSuccess = clearOnSuccess;
        }

        @Override public void success(@NonNull Call<MwQueryResponse> call,
                                      @NonNull SearchResults results) {
            List<SearchResult> resultList = results.getResults();
            cache(resultList, searchTerm);
            log(resultList, startTime);

            if (!isAdded()) {
                return;
            }
            if (clearOnSuccess) {
                clearResults(false);
            }
            updateProgressBar(false);
            searchErrorView.setVisibility(View.GONE);

            // full text special:
            SearchResultsFragment.this.lastFullTextResults = results;

            displayResults(resultList);
        }

        @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                      @NonNull Throwable caught) {
            // If there's an error, just log it and let the existing prefix search results be.
            logError(true, startTime);
            if (isAdded()) {
                updateProgressBar(false);
            }
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}

