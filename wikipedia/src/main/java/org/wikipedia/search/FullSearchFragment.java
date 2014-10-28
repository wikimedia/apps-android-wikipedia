package org.wikipedia.search;

import org.wikipedia.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.wikidata.WikidataDescriptionsTask;
import org.wikipedia.wikidata.WikidataSite;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FullSearchFragment extends Fragment {
    private static final int DELAY_MILLIS = 1000;
    private static final int MAX_CACHE_SIZE_IMAGES = 48;
    private static final int MAX_CACHE_SIZE_DESCRIPTIONS = 96;
    private static final int MESSAGE_SEARCH = 1;

    private WikipediaApp app;

    private SearchArticlesFragment searchFragment;
    private View searchResultsContainer;
    private ListView searchResultsList;
    private SearchResultAdapter adapter;
    private String currentSearchTerm = "";

    private View searchFullContainer;
    private View searchNetworkError;
    private View searchNoResults;
    private TextView searchSuggestion;

    private Handler searchHandler;

    private FullSearchArticlesTask.FullSearchResults lastResults;
    private List<FullSearchResult> totalResults;

    private ParcelableLruCache<String> descriptionCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_DESCRIPTIONS, String.class);
    private ParcelableLruCache<String> pageImagesCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_IMAGES, String.class);

    public FullSearchFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_full, container, false);
        searchFullContainer = rootView.findViewById(R.id.search_full_container);
        searchFragment = (SearchArticlesFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.search_fragment);

        searchResultsContainer = rootView.findViewById(R.id.search_results_container);
        searchResultsList = (ListView) rootView.findViewById(R.id.full_search_results_list);

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = ((FullSearchResult) searchResultsList.getAdapter().getItem(position)).getTitle();
                searchFragment.navigateToTitle(title);
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        searchSuggestion = (TextView) rootView.findViewById(R.id.search_suggestion);
        searchSuggestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String suggestion = (String) searchSuggestion.getTag();
                if (suggestion != null) {
                    searchFragment.setSearchText(suggestion);
                    startSearch(suggestion, true);
                }
            }
        });

        searchNoResults = rootView.findViewById(R.id.search_results_empty);

        searchNetworkError = rootView.findViewById(R.id.search_network_error);
        searchNetworkError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSearch(currentSearchTerm, 0);
                searchNetworkError.setVisibility(View.GONE);
            }
        });

        totalResults = new ArrayList<FullSearchResult>();
        adapter.setResults(totalResults);

        searchHandler = new Handler(new SearchHandlerCallback());

        return rootView;
    }

    public void show() {
        searchFullContainer.setVisibility(View.VISIBLE);
    }

    public void hide() {
        searchFullContainer.setVisibility(View.GONE);
    }

    public boolean isShowing() {
        return searchFullContainer.getVisibility() == View.VISIBLE;
    }

    private class SearchHandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            final String mySearchTerm = (String) msg.obj;
            doSearch(mySearchTerm, 0);
            return true;
        }
    }

    /**
     * Kick off a search, based on a given search term.
     * @param term Phrase to search for.
     * @param force Whether to "force" starting this search. If the search is not forced, the
     *              search may be delayed by a small time, so that network requests are not sent
     *              too often.  If the search is forced, the network request is sent immediately.
     */
    public void startSearch(String term, boolean force) {
        if (currentSearchTerm.equals(term)) {
            return;
        }

        currentSearchTerm = term;

        searchNoResults.setVisibility(View.GONE);
        searchNetworkError.setVisibility(View.GONE);
        searchSuggestion.setVisibility(View.GONE);

        searchHandler.removeMessages(MESSAGE_SEARCH);
        Message searchMessage = Message.obtain();
        searchMessage.what = MESSAGE_SEARCH;
        searchMessage.obj = term;

        if (force) {
            searchHandler.sendMessage(searchMessage);
        } else {
            searchHandler.sendMessageDelayed(searchMessage, DELAY_MILLIS);
        }
    }

    private void doSearch(final String searchTerm, final int continueOffset) {
        (new FullSearchArticlesTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm, continueOffset) {
            @Override
            public void onFinish(FullSearchResults results) {
                if (!isAdded()) {
                    return;
                }
                lastResults = results;
                totalResults.addAll(lastResults.getResults());

                ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetChanged();

                if (results.getSuggestion().length() > 0) {
                    searchSuggestion.setText(Html.fromHtml("<u>"
                            + String.format(getString(R.string.search_did_you_mean), results.getSuggestion())
                            + "</u>"));
                    searchSuggestion.setTag(results.getSuggestion());
                    searchSuggestion.setVisibility(View.VISIBLE);
                }
                searchResultsContainer.setVisibility(View.VISIBLE);

                ((PageActivity)getActivity()).setSupportProgressBarVisibility(false);
                if (lastResults.getResults().size() == 0) {
                    searchNoResults.setVisibility(View.VISIBLE);
                    searchResultsList.setVisibility(View.GONE);
                } else {
                    searchResultsList.setVisibility(View.VISIBLE);
                    getWikidataDescriptions(lastResults.getResults());
                    getPageThumbnails(lastResults.getResults());
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                ((PageActivity)getActivity()).setSupportProgressBarVisibility(false);

                if (continueOffset == 0) {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchNetworkError.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.error_network_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBeforeExecute() {
                ((PageActivity)getActivity()).setSupportProgressBarIndeterminate(true);
                ((PageActivity)getActivity()).setSupportProgressBarVisibility(true);
                if (continueOffset == 0) {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchNoResults.setVisibility(View.GONE);
                    searchNetworkError.setVisibility(View.GONE);
                    searchSuggestion.setVisibility(View.GONE);

                    totalResults.clear();
                    ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetChanged();
                }
            }
        }).execute();
    }

    private void getWikidataDescriptions(List<FullSearchResult> results) {
        List<String> idList = new ArrayList<String>();
        for (FullSearchResult r : results) {
            if (descriptionCache.get(r.getTitle().getPrefixedText()) == null) {
                // not in our cache yet
                idList.add(r.getWikiBaseId());
            }
        }
        if (idList.isEmpty()) {
            return;
        }

        WikidataDescriptionsTask descriptionTask = new WikidataDescriptionsTask(
                app.getAPIForSite(new WikidataSite()),
                app.getPrimaryLanguage(),
                idList) {
            @Override
            public void onFinish(Map<String, String> result) {
                for (Map.Entry<String, String> entry : result.entrySet()) {
                    if (entry.getValue() == null) {
                        continue;
                    }
                    descriptionCache.put(entry.getKey(), entry.getValue());
                }
                ((BaseAdapter) searchResultsList.getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onCatch(Throwable caught) {
                // Don't actually do anything.
                // Descriptions are expendable
            }
        };
        descriptionTask.execute();
    }

    private void getPageThumbnails(List<FullSearchResult> results) {
        List<PageTitle> titleList = new ArrayList<PageTitle>();
        for (FullSearchResult r : results) {
            if (pageImagesCache.get(r.getTitle().getPrefixedText()) == null) {
                // not in our cache yet
                titleList.add(r.getTitle());
            }
        }
        if (titleList.isEmpty()) {
            return;
        }

        PageImagesTask imagesTask = new PageImagesTask(
                app.getAPIForSite(app.getPrimarySite()),
                app.getPrimarySite(),
                titleList,
                (int)(WikipediaApp.PREFERRED_THUMB_SIZE * WikipediaApp.getInstance().getScreenDensity())) {
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
            FullSearchResult result = (FullSearchResult) getItem(position);
            pageTitleText.setText(result.getTitle().getDisplayText());

            String wikidataId = result.getWikiBaseId();
            if (!TextUtils.isEmpty(wikidataId)) {
                TextView descriptionText = (TextView) convertView.findViewById(R.id.result_description);
                descriptionText.setText(descriptionCache.get(wikidataId));
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
                doSearch(currentSearchTerm, lastResults.getContinueOffset());
            }

            return convertView;
        }
    }
}

