package org.wikipedia.search;

import org.wikipedia.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;

import com.squareup.picasso.Picasso;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class TitleSearchFragment extends Fragment {
    private static final int DELAY_MILLIS = 300;
    private static final int MAX_CACHE_SIZE_SEARCH_RESULTS = 4;
    private static final int MAX_CACHE_SIZE_IMAGES = 48;
    private static final int MESSAGE_SEARCH = 1;
    private static final String ARG_RESULTS_CACHE = "searchResultsCache";
    private static final String ARG_PAGE_IMAGES_CACHE = "pageImagesCache";

    private WikipediaApp app;
    private SearchResultAdapter adapter;
    private SearchArticlesFragment searchFragment;

    private View searchTitlesContainer;
    private ListView searchResultsList;
    private View searchNetworkError;
    private View searchNoResults;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache
            = new ParcelableLruCache<List<PageTitle>>(MAX_CACHE_SIZE_SEARCH_RESULTS, List.class);
    private ParcelableLruCache<String> pageImagesCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_IMAGES, String.class);
    private String currentSearchTerm = "";

    private Handler searchHandler;

    private TitleSearchTask curSearchTask;

    public TitleSearchFragment() {
    }

    /**
     * Interface for receiving an event when a Title search returns no results.
     * Will be useful for automatically switching to a Full search
     */
    public interface OnNoResultsListener {
        void onNoResults();
    }

    private OnNoResultsListener onNoResultsListener;
    public void setOnNoResultsListener(OnNoResultsListener listener) {
        onNoResultsListener = listener;
    }

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private void displayResults(List<PageTitle> results) {
        adapter.setResults(results);

        if (results.size() == 0) {
            searchNoResults.setVisibility(View.VISIBLE);
            searchResultsList.setVisibility(View.GONE);
        } else {
            searchNoResults.setVisibility(View.GONE);
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
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (WikipediaApp)getActivity().getApplicationContext();
        LinearLayout parentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_search_title, container, false);
        searchTitlesContainer = parentLayout.findViewById(R.id.search_title_container);
        searchFragment = (SearchArticlesFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.search_fragment);

        if (savedInstanceState != null) {
            ParcelableLruCache<List<PageTitle>> mySearchResultsCache = savedInstanceState.getParcelable(ARG_RESULTS_CACHE);
            if (mySearchResultsCache != null) {
                searchResultsCache = mySearchResultsCache;
                pageImagesCache = savedInstanceState.getParcelable(ARG_PAGE_IMAGES_CACHE);
            }
        }

        searchResultsList = (ListView) parentLayout.findViewById(R.id.search_results_list);
        searchNetworkError = parentLayout.findViewById(R.id.search_network_error);
        searchNoResults = parentLayout.findViewById(R.id.search_results_empty);

        searchHandler = new Handler(new SearchHandlerCallback());

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) searchResultsList.getAdapter().getItem(position);
                searchFragment.navigateToTitle(title);
            }
        });

        searchNetworkError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch(currentSearchTerm, true); //just retry!
                searchNetworkError.setVisibility(View.GONE);
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        return parentLayout;
    }

    public void show() {
        searchTitlesContainer.setVisibility(View.VISIBLE);
    }

    public void hide() {
        searchTitlesContainer.setVisibility(View.GONE);
    }

    public boolean isShowing() {
        return searchTitlesContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_RESULTS_CACHE, searchResultsCache);
        outState.putParcelable(ARG_PAGE_IMAGES_CACHE, pageImagesCache);
    }

    /**
     * Kick off a search, based on a given search term.
     * @param term Phrase to search for.
     * @param force Whether to "force" starting this search. If the search is not forced, the
     *              search may be delayed by a small time, so that network requests are not sent
     *              too often.  If the search is forced, the network request is sent immediately.
     */
    public void startSearch(String term, boolean force) {
        if (TextUtils.isEmpty(term)) {
            return;
        }
        if (currentSearchTerm.equals(term) && !force) {
            return;
        }

        searchNoResults.setVisibility(View.GONE);
        searchNetworkError.setVisibility(View.GONE);

        currentSearchTerm = term;

        List<PageTitle> cacheResult = searchResultsCache.get(app.getPrimaryLanguage() + "-" + term);
        if (cacheResult != null) {
            displayResults(cacheResult);
            return;
        }

        searchHandler.removeMessages(MESSAGE_SEARCH);
        Message searchMessage = Message.obtain();
        searchMessage.what = MESSAGE_SEARCH;
        searchMessage.obj = term;

        searchHandler.sendMessageDelayed(searchMessage, DELAY_MILLIS);
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

            // highlight search term within the text
            String highlightedString = title.getDisplayText();
            if (currentSearchTerm.length() > 0 && currentSearchTerm.length() <= highlightedString.length()) {
                highlightedString = "<strong>"
                        + highlightedString.substring(0, currentSearchTerm.length())
                        + "</strong>"
                        + highlightedString.substring(currentSearchTerm.length(), highlightedString.length());
            }
            pageTitleText.setText(Html.fromHtml(highlightedString));
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
            final long startMillis = System.currentTimeMillis();
            TitleSearchTask searchTask = new TitleSearchTask(app, app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), mySearchTerm) {
                @Override
                public void onBeforeExecute() {
                    ((PageActivity)getActivity()).updateProgressBar(true, true, 0);
                }

                @Override
                public void onFinish(List<PageTitle> result) {
                    if (!isAdded()) {
                        return;
                    }
                    searchFragment.getFunnel().searchResults(false, result.size(), (int)(System.currentTimeMillis() - startMillis));
                    ((PageActivity)getActivity()).updateProgressBar(false, true, 0);
                    searchNetworkError.setVisibility(View.GONE);
                    displayResults(result);
                    searchResultsCache.put(app.getPrimaryLanguage() + "-" + mySearchTerm, result);
                    curSearchTask = null;
                    if (result.size() == 0 && onNoResultsListener != null) {
                        onNoResultsListener.onNoResults();
                    }
                    // scroll to top, but post it to the message queue, because it should be done
                    // after the data set is updated.
                    searchResultsList.post(new Runnable() {
                        @Override
                        public void run() {
                            searchResultsList.setSelectionAfterHeaderView();
                        }
                    });
                }

                @Override
                public void onCatch(Throwable caught) {
                    if (!isAdded()) {
                        return;
                    }
                    searchFragment.getFunnel().searchError(false, (int)(System.currentTimeMillis() - startMillis));
                    ((PageActivity)getActivity()).updateProgressBar(false, true, 0);
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
