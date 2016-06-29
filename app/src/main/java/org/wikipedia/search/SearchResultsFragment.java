package org.wikipedia.search;

import org.wikipedia.ParcelableLruCache;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.MainActivity;
import org.wikipedia.page.MainActivityLongPressHandler;
import org.wikipedia.page.PageLongPressHandler;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;

import com.facebook.drawee.view.SimpleDraweeView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public class SearchResultsFragment extends Fragment {
    private static final int BATCH_SIZE = 20;
    private static final int DELAY_MILLIS = 300;
    private static final int MESSAGE_SEARCH = 1;
    private static final int MAX_CACHE_SIZE_SEARCH_RESULTS = 4;
    private static final String ARG_RESULTS_CACHE = "searchResultsCache";
    /**
     * Constant to ease in the conversion of timestamps from nanoseconds to milliseconds.
     */
    private static final int NANO_TO_MILLI = 1_000_000;

    private SearchArticlesFragment searchFragment;
    private View searchResultsDisplay;
    private View searchResultsContainer;
    private ListView searchResultsList;
    private WikiErrorView searchErrorView;
    private View searchNoResults;
    private TextView searchSuggestion;

    private WikipediaApp app;
    @NonNull private ParcelableLruCache<List<SearchResult>> searchResultsCache
            = new ParcelableLruCache<>(MAX_CACHE_SIZE_SEARCH_RESULTS, List.class);
    private Handler searchHandler;
    private TitleSearchTask curSearchTask;
    private String currentSearchTerm = "";
    @Nullable private SearchResults lastFullTextResults;
    @NonNull private final List<SearchResult> totalResults = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_results, container, false);
        searchResultsDisplay = rootView.findViewById(R.id.search_results_display);
        searchFragment = (SearchArticlesFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.search_fragment);

        searchResultsContainer = rootView.findViewById(R.id.search_results_container);
        searchResultsList = (ListView) rootView.findViewById(R.id.search_results_list);

        if (savedInstanceState != null) {
            ParcelableLruCache<List<SearchResult>> mySearchResultsCache = savedInstanceState.getParcelable(ARG_RESULTS_CACHE);
            if (mySearchResultsCache != null) {
                searchResultsCache = mySearchResultsCache;
            }
        }

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle item = ((SearchResult) getAdapter().getItem(position)).getPageTitle();
                searchFragment.navigateToTitle(item, false, position);
            }
        });

        SearchResultAdapter adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        searchSuggestion = (TextView) rootView.findViewById(R.id.search_suggestion);
        searchSuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String suggestion = (String) searchSuggestion.getTag();
                if (suggestion != null) {
                    searchFragment.getFunnel().searchDidYouMean();
                    searchFragment.setSearchText(suggestion);
                    startSearch(suggestion, true);
                }
            }
        });

        searchNoResults = rootView.findViewById(R.id.search_results_empty);

        searchErrorView = (WikiErrorView) rootView.findViewById(R.id.search_error_view);
        searchErrorView.setRetryClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchErrorView.setVisibility(View.GONE);
                startSearch(currentSearchTerm, true);
            }
        });

        searchHandler = new Handler(new SearchHandlerCallback());

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PageLongPressHandler.ListViewContextMenuListener contextMenuListener = new LongPressHandler(getMainActivity());
        new PageLongPressHandler(getActivity(), searchResultsList, HistoryEntry.SOURCE_SEARCH,
                contextMenuListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_RESULTS_CACHE, searchResultsCache);
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
    public void startSearch(String term, boolean force) {
        if (!force && currentSearchTerm.equals(term)) {
            return;
        }

        cancelSearchTask();
        currentSearchTerm = term;

        if (term.isEmpty()) {
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
            doTitlePrefixSearch(mySearchTerm);
            return true;
        }
    }

    private void doTitlePrefixSearch(final String searchTerm) {
        // Use nanoTime to measure the time the search was started.
        final long startTime = System.nanoTime();
        TitleSearchTask searchTask = new TitleSearchTask(app.getAPIForSite(app.getSite()), app.getSite(), searchTerm) {
            @Override
            public void onBeforeExecute() {
                getMainActivity().updateProgressBar(true, true, 0);
            }

            @Override
            public void onFinish(SearchResults results) {
                if (!isAdded()) {
                    return;
                }
                List<SearchResult> resultList = results.getResults();
                // To ease data analysis and better make the funnel track with user behaviour,
                // only transmit search results events if there are a nonzero number of results
                if (!resultList.isEmpty()) {
                    // Calculate total time taken to display results, in milliseconds
                    final int timeToDisplay = (int) ((System.nanoTime() - startTime) / NANO_TO_MILLI);
                    searchFragment.getFunnel().searchResults(false, resultList.size(), timeToDisplay);
                }

                getMainActivity().updateProgressBar(false, true, 0);
                searchErrorView.setVisibility(View.GONE);
                if (!resultList.isEmpty()) {
                    clearResults();
                    displayResults(resultList);
                }

                // add titles to cache...
                searchResultsCache.put(app.getAppOrSystemLanguageCode() + "-" + searchTerm, resultList);
                curSearchTask = null;

                final String suggestion = results.getSuggestion();
                if (!suggestion.isEmpty()) {
                    searchSuggestion.setText(Html.fromHtml("<u>"
                            + String.format(getString(R.string.search_did_you_mean), suggestion)
                            + "</u>"));
                    searchSuggestion.setTag(suggestion);
                    searchSuggestion.setVisibility(View.VISIBLE);
                } else {
                    searchSuggestion.setVisibility(View.GONE);
                }

                // scroll to top, but post it to the message queue, because it should be done
                // after the data set is updated.
                searchResultsList.post(new Runnable() {
                    @Override
                    public void run() {
                        searchResultsList.setSelectionAfterHeaderView();
                    }
                });

                if (resultList.isEmpty()) {
                    // kick off full text search if we get no results
                    doFullTextSearch(currentSearchTerm, null, true);
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                // Calculate total time taken to display results, in milliseconds
                final int timeToDisplay = (int) ((System.nanoTime() - startTime) / NANO_TO_MILLI);
                searchFragment.getFunnel().searchError(false, timeToDisplay);
                getMainActivity().updateProgressBar(false, true, 0);

                searchErrorView.setVisibility(View.VISIBLE);
                searchErrorView.setError(caught);

                searchResultsContainer.setVisibility(View.GONE);
                curSearchTask = null;
            }
        };

        cancelSearchTask();
        curSearchTask = searchTask;
        searchTask.execute();
    }

    private void cancelSearchTask() {
        getMainActivity().updateProgressBar(false, true, 0);
        searchHandler.removeMessages(MESSAGE_SEARCH);
        if (curSearchTask != null) {
            // This does not cancel the HTTP request itself
            // But it does cancel the execution of onFinish
            // This makes sure that a slower previous search query does not override
            // the results of a newer search query
            curSearchTask.cancel();
        }
    }

    private void doFullTextSearch(final String searchTerm,
                                  final SearchResults.ContinueOffset continueOffset,
                                  final boolean clearOnSuccess) {
        // Use nanoTime to measure the time the search was started.
        final long startTime = System.nanoTime();
        new FullSearchArticlesTask(app.getAPIForSite(app.getSite()), app.getSite(),
                                   searchTerm, BATCH_SIZE, continueOffset, false) {
            @Override
            public void onBeforeExecute() {
                getMainActivity().updateProgressBar(true, true, 0);
            }

            @Override
            public void onFinish(SearchResults results) {
                if (!isAdded()) {
                    return;
                }

                if (clearOnSuccess) {
                    clearResults(false);
                }

                // To ease data analysis and better make the funnel track with user behaviour,
                // only transmit search results events if there are a nonzero number of results
                final List<SearchResult> resultList = results.getResults();
                if (!resultList.isEmpty()) {
                    // Calculate total time taken to display results, in milliseconds
                    final int timeToDisplay = (int) ((System.nanoTime() - startTime) / NANO_TO_MILLI);
                    searchFragment.getFunnel().searchResults(true, resultList.size(), timeToDisplay);
                }

                // append results to cache...
                List<SearchResult> cachedTitles = searchResultsCache.get(app.getAppOrSystemLanguageCode() + "-" + searchTerm);
                if (cachedTitles != null) {
                    cachedTitles.addAll(resultList);
                }

                getMainActivity().updateProgressBar(false, true, 0);
                searchErrorView.setVisibility(View.GONE);

                // full text special:
                SearchResultsFragment.this.lastFullTextResults = results;

                displayResults(resultList);
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                // Calculate total time taken to display results, in milliseconds
                final int timeToDisplay = (int) ((System.nanoTime() - startTime) / NANO_TO_MILLI);
                searchFragment.getFunnel().searchError(true, timeToDisplay);
                getMainActivity().updateProgressBar(false, true, 0);

                // since this is a follow-up search just show a message
                FeedbackUtil.showError(getView(), caught);
            }
        }.execute();
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

    private void clearResults(boolean clearSuggestion) {
        searchResultsContainer.setVisibility(View.GONE);
        searchNoResults.setVisibility(View.GONE);
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

    // TODO: don't assume host is MainActivity. Use Fragment callbacks pattern.
    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private void displayResults(List<SearchResult> results) {
        for (SearchResult newResult : results) {
            if (!totalResults.contains(newResult)) {
                totalResults.add(newResult);
            }
        }

        searchResultsContainer.setVisibility(View.VISIBLE);
        if (totalResults.size() == 0) {
            searchNoResults.setVisibility(View.VISIBLE);
            searchResultsList.setVisibility(View.GONE);
        } else {
            searchNoResults.setVisibility(View.GONE);
            searchResultsList.setVisibility(View.VISIBLE);
        }

        getAdapter().notifyDataSetChanged();
    }

    private class LongPressHandler extends MainActivityLongPressHandler
            implements PageLongPressHandler.ListViewContextMenuListener {
        private int lastPositionRequested;

        LongPressHandler(@NonNull MainActivity activity) {
            super(activity);
        }

        @Override
        public PageTitle getTitleForListPosition(int position) {
            lastPositionRequested = position;
            return ((SearchResult) getAdapter().getItem(position)).getPageTitle();
        }

        @Override
        public void onOpenLink(PageTitle title, HistoryEntry entry) {
            searchFragment.navigateToTitle(title, false, lastPositionRequested);
        }

        @Override
        public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
            searchFragment.navigateToTitle(title, true, lastPositionRequested);
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
            TextView pageTitleText = (TextView) convertView.findViewById(R.id.page_list_item_title);
            SearchResult result = (SearchResult) getItem(position);

            GoneIfEmptyTextView descriptionText = (GoneIfEmptyTextView) convertView.findViewById(R.id.page_list_item_description);
            descriptionText.setText(result.getPageTitle().getDescription());

            View redirectContainer = convertView.findViewById(R.id.page_list_item_redirect_container);
            if (TextUtils.isEmpty(result.getRedirectFrom())) {
                redirectContainer.setVisibility(View.GONE);
                descriptionText.setVisibility(View.VISIBLE);
            } else {
                redirectContainer.setVisibility(View.VISIBLE);
                descriptionText.setVisibility(View.GONE);
                TextView redirectText = (TextView) convertView.findViewById(R.id.page_list_item_redirect);
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
                pageTitleText.setText(Html.fromHtml(displayText));
            } else {
                pageTitleText.setText(displayText);
            }

            ViewUtil.loadImageUrlInto((SimpleDraweeView) convertView.findViewById(R.id.page_list_item_image),
                    result.getPageTitle().getThumbUrl());

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == (totalResults.size() - 1)) {
                if (lastFullTextResults == null) {
                    // the first full text search
                    doFullTextSearch(currentSearchTerm, null, false);
                } else if (lastFullTextResults.getContinueOffset() != null) {
                    // subsequent full text searches
                    doFullTextSearch(currentSearchTerm, lastFullTextResults.getContinueOffset(), false);
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
}

