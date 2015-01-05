package org.wikipedia.search;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import com.squareup.picasso.Picasso;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class FullSearchFragment extends Fragment {
    private static final int BATCH_SIZE = 12;
    private static final int DELAY_MILLIS = 1000;
    private static final int MESSAGE_SEARCH = 1;

    private WikipediaApp app;

    private SearchArticlesFragment searchFragment;
    private View searchResultsContainer;
    private ListView searchResultsList;
    private String currentSearchTerm = "";

    private View searchFullContainer;
    private View searchNetworkError;
    private View searchNoResults;
    private TextView searchSuggestion;

    private Handler searchHandler;

    private FullSearchArticlesTask.FullSearchResults lastResults;
    private List<PageTitle> totalResults;

    public FullSearchFragment() {
    }

    @Override
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
                PageTitle item = (PageTitle) searchResultsList.getAdapter().getItem(position);
                // always add the description of the item to the cache so we don't even try to get it again
                app.getWikidataCache().put(item.toString(), item.getDescription());
                searchFragment.navigateToTitle(item);
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

        searchNetworkError = rootView.findViewById(R.id.search_network_error);
        searchNetworkError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSearch(currentSearchTerm, null);
                searchNetworkError.setVisibility(View.GONE);
            }
        });

        totalResults = new ArrayList<PageTitle>();
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
            if (!isAdded()) {
                return true;
            }
            final String mySearchTerm = (String) msg.obj;
            doSearch(mySearchTerm, null);
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

    private void doSearch(final String searchTerm, final FullSearchArticlesTask.ContinueOffset continueOffset) {
        final long startMillis = System.currentTimeMillis();
        new FullSearchArticlesTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm, BATCH_SIZE, continueOffset) {
            @Override
            public void onFinish(FullSearchResults results) {
                if (!isAdded()) {
                    return;
                }
                searchFragment.getFunnel().searchResults(true, results.getResults().size(), (int)(System.currentTimeMillis() - startMillis));
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

                ((PageActivity)getActivity()).updateProgressBar(false, true, 0);
                if (lastResults.getResults().size() == 0 && continueOffset == null) {
                    searchNoResults.setVisibility(View.VISIBLE);
                    searchResultsList.setVisibility(View.GONE);
                } else {
                    searchResultsList.setVisibility(View.VISIBLE);
                }

                if (continueOffset == null) {
                    // scroll to top, but post it to the message queue, because it should be done
                    // after the data set is updated.
                    searchResultsList.post(new Runnable() {
                        @Override
                        public void run() {
                            searchResultsList.setSelectionAfterHeaderView();
                        }
                    });
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                searchFragment.getFunnel().searchError(true, (int)(System.currentTimeMillis() - startMillis));
                ((PageActivity)getActivity()).updateProgressBar(false, true, 0);

                if (continueOffset == null) {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchNetworkError.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.error_network_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onBeforeExecute() {
                ((PageActivity)getActivity()).updateProgressBar(true, true, 0);
                if (continueOffset == null) {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchNoResults.setVisibility(View.GONE);
                    searchNetworkError.setVisibility(View.GONE);
                    searchSuggestion.setVisibility(View.GONE);

                    totalResults.clear();
                    ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetChanged();
                }
            }
        }.execute();
    }

    public PageTitle getFirstSuggestion() {
        Adapter adapter = searchResultsList.getAdapter();
        if (adapter.getCount() > 0) {
            return (PageTitle) adapter.getItem(0);
        } else {
            return null;
        }
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
            TextView pageTitleText = (TextView) convertView.findViewById(R.id.result_title);
            PageTitle result = (PageTitle) getItem(position);
            pageTitleText.setText(result.getDisplayText());

            TextView descriptionText = (TextView) convertView.findViewById(R.id.result_description);
            descriptionText.setText(result.getDescription());

            ImageView imageView = (ImageView) convertView.findViewById(R.id.result_image);
            String thumbnail = result.getThumbUrl();
            if (app.showImages() && thumbnail != null) {
                Picasso.with(parent.getContext())
                        .load(thumbnail)
                        .placeholder(R.drawable.ic_pageimage_placeholder)
                        .error(R.drawable.ic_pageimage_placeholder)
                        .into(imageView);
            } else {
                Picasso.with(getActivity())
                       .load(R.drawable.ic_pageimage_placeholder)
                       .into(imageView);
            }

            // ...and lastly, if we've scrolled to the last item in the list, then
            // continue searching!
            if (position == results.size() - 1 && lastResults.getContinueOffset() != null) {
                doSearch(currentSearchTerm, lastResults.getContinueOffset());
            }

            return convertView;
        }
    }
}

