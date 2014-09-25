package org.wikipedia.search;

import org.wikipedia.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.pageimages.PageImagesTask;
import com.squareup.picasso.Picasso;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FullSearchFragment extends Fragment {
    private static final String KEY_SEARCH_TERM = "searchTerm";

    private WikipediaApp app;

    private View searchResultsContainer;
    private ListView searchResultsList;
    private SearchResultAdapter adapter;
    private TextView resultsStatusText;
    private String searchTermCurrent;
    private String searchTermOriginal;

    private View searchFailedContainer;
    private TextView searchFailedText;
    private View searchRetryButton;
    private View searchProgressBar;

    private FullSearchArticlesTask.FullSearchResults lastResults;
    private List<FullSearchResult> totalResults;

    private static final int THUMB_SIZE_DP = 96;
    private static final int MAX_CACHE_SIZE_IMAGES = 48;

    private ParcelableLruCache<String> pageImagesCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_IMAGES, String.class);

    public static FullSearchFragment newInstance(String searchTerm) {
        FullSearchFragment f = new FullSearchFragment();
        Bundle args = new Bundle();
        args.putString(KEY_SEARCH_TERM, searchTerm);
        f.setArguments(args);
        return f;
    }

    public FullSearchFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        setHasOptionsMenu(true);
        searchTermOriginal = getArguments().getString(KEY_SEARCH_TERM);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_results, container, false);

        searchResultsContainer = rootView.findViewById(R.id.search_results_container);
        searchResultsList = (ListView) rootView.findViewById(R.id.full_search_results_list);

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = ((FullSearchResult) searchResultsList.getAdapter().getItem(position)).getTitle();
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
                app.getBus().post(new NewWikiPageNavigationEvent(title, historyEntry));
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        resultsStatusText = (TextView) rootView.findViewById(R.id.search_results_status);
        searchFailedText = (TextView) rootView.findViewById(R.id.search_failed_text);
        searchFailedContainer = rootView.findViewById(R.id.search_failed_container);

        searchRetryButton = rootView.findViewById(R.id.search_retry_button);
        searchRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSearch(searchTermCurrent, false, 0);
            }
        });

        searchProgressBar = rootView.findViewById(R.id.search_progress_bar);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        totalResults = new ArrayList<FullSearchResult>();
        adapter.setResults(totalResults);

        // if we already have a search term, then we must have been restored from state or backstack
        if (!TextUtils.isEmpty(searchTermOriginal)) {
            doSearch(searchTermOriginal, false, 0);
        }
    }

    public void newSearch(final String searchTerm) {
        //update this fragment's arguments...
        getArguments().putString(KEY_SEARCH_TERM, searchTerm);
        //if we're already attached, then do the search...
        if (isAdded()) {
            doSearch(searchTerm, false, 0);
        }
    }

    private void doSearch(final String searchTerm, final boolean fromSuggestion, final int continueOffset) {
        if (!fromSuggestion) {
            this.searchTermCurrent = searchTerm;
            this.searchTermOriginal = searchTerm;
        }

        (new FullSearchArticlesTask(app, app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm, continueOffset) {
            @Override
            public void onFinish(FullSearchResults results) {
                if (!isAdded()) {
                    return;
                }
                lastResults = results;
                totalResults.addAll(lastResults.getResults());

                ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetChanged();

                searchProgressBar.setVisibility(View.GONE);
                if (lastResults.getResults().size() == 0) {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchFailedContainer.setVisibility(View.VISIBLE);
                    searchFailedText.setText(app.getString(R.string.search_no_results, searchTermCurrent));
                    searchRetryButton.setVisibility(View.GONE);
                } else {
                    searchResultsContainer.setVisibility(View.VISIBLE);
                    searchFailedContainer.setVisibility(View.GONE);

                    if (continueOffset == 0) {
                        if (fromSuggestion) {
                            resultsStatusText.setVisibility(View.VISIBLE);
                            resultsStatusText.setText(app.getString(R.string.search_showing_instead, searchTermOriginal, searchTerm));
                        } else {
                            resultsStatusText.setVisibility(View.GONE);
                        }
                    }

                    getPageThumbnails(lastResults.getResults());
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                if (caught instanceof FullSearchSuggestionException) {
                    if (!fromSuggestion) {
                        searchTermCurrent = ((FullSearchSuggestionException) caught).getSuggestion();
                        doSearch(searchTermCurrent, true, 0);
                    }
                } else {
                    if (continueOffset == 0) {
                        searchProgressBar.setVisibility(View.GONE);
                        searchResultsContainer.setVisibility(View.GONE);
                        searchFailedContainer.setVisibility(View.VISIBLE);
                        searchFailedText.setText(app.getString(R.string.error_network_error));
                        searchRetryButton.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(getActivity(), getString(R.string.error_network_error), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onBeforeExecute() {
                if (continueOffset == 0) {
                    searchProgressBar.setVisibility(View.VISIBLE);
                    searchResultsContainer.setVisibility(View.GONE);
                    searchFailedContainer.setVisibility(View.GONE);
                }
            }
        }).execute();
    }

    private void getPageThumbnails(List<FullSearchResult> results) {
        List<PageTitle> titleList = new ArrayList<PageTitle>();
        for (FullSearchResult r : results) {
            titleList.add(r.getTitle());
        }
        PageImagesTask imagesTask = new PageImagesTask(
                app.getAPIForSite(app.getPrimarySite()),
                app.getPrimarySite(),
                titleList,
                (int)(THUMB_SIZE_DP * WikipediaApp.getInstance().getScreenDensity())) {
            @Override
            public void onFinish(Map<PageTitle, String> result) {
                for (Map.Entry<PageTitle, String> entry : result.entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }
                    pageImagesCache.put(entry.getKey().getPrefixedText(), entry.getValue());
                }
                ((BaseAdapter) searchResultsList.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onCatch(Throwable caught) {
                // Don't actually do anything.
                // Thumbnails are expendable
            }
        };
        imagesTask.execute();
    }

    private final class SearchResultAdapter extends BaseAdapter {
        private List<FullSearchResult> results;
        private final LayoutInflater inflater;

        private SearchResultAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        private List<FullSearchResult> getResults() {
            return results;
        }

        private void setResults(List<FullSearchResult> results) {
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
                convertView = inflater.inflate(R.layout.item_full_search_result, parent, false);
            }
            TextView pageTitleText = (TextView) convertView.findViewById(R.id.result_title);
            TextView pageSnippetText = (TextView) convertView.findViewById(R.id.result_snippet);
            TextView pageRedirectText = (TextView) convertView.findViewById(R.id.result_title_redirect);
            FullSearchResult result = (FullSearchResult) getItem(position);
            pageTitleText.setText(result.getTitle().getDisplayText());
            pageSnippetText.setText(Html.fromHtml(result.getSnippet()));
            if (result.getRedirectSnippet() != null && result.getRedirectSnippet().length() > 0) {
                pageRedirectText.setText(Html.fromHtml(app.getString(R.string.search_redirect_title, result.getRedirectSnippet())));
                pageRedirectText.setVisibility(View.VISIBLE);
            } else {
                pageRedirectText.setVisibility(View.GONE);
            }

            ImageView imageView = (ImageView) convertView.findViewById(R.id.result_image);
            String thumbnail = pageImagesCache.get(result.getTitle().getPrefixedText());
            if (thumbnail == null) {
                Picasso.with(parent.getContext())
                        .load(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            } else {
                Picasso.with(parent.getContext())
                        .load(thumbnail)
                        .placeholder(R.drawable.ic_pageimage_placeholder)
                        .error(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            }

            //...and lastly, if we've scrolled to the last item in the list, then
            //continue searching!
            if (position == results.size() - 1 && lastResults.getContinueOffset() > 0) {
                doSearch(searchTermCurrent, false, lastResults.getContinueOffset());
            }

            return convertView;
        }
    }

}

