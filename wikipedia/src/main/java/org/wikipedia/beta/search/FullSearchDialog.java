package org.wikipedia.beta.search;

import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.*;
import com.squareup.picasso.Picasso;
import org.wikipedia.beta.*;
import org.wikipedia.beta.pageimages.PageImagesTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FullSearchDialog extends Dialog {

    private WikipediaApp app;

    private SearchArticlesFragment parentFragment;
    private View searchResultsContainer;
    private ListView searchResultsList;
    private SearchResultAdapter adapter;
    private TextView resultsStatusText;
    private String searchTerm;
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

    public FullSearchDialog(SearchArticlesFragment parent, String searchTerm) {
        super(parent.getActivity());
        this.parentFragment = parent;
        this.searchTerm = searchTerm;
        this.searchTermOriginal = searchTerm;
        app = WikipediaApp.getInstance();

        LayoutInflater inflater = (LayoutInflater) parent.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dlgLayout = inflater.inflate(R.layout.dialog_search_results, null);
        setContentView(dlgLayout);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(lp);
        //getWindow().setBackgroundDrawableResource(Utils.getThemedAttributeId(parent.getActivity(), R.attr.window_background_color));

        setTitle(app.getString(R.string.search_results_title));

        searchResultsContainer = dlgLayout.findViewById(R.id.search_results_container);
        searchResultsList = (ListView) dlgLayout.findViewById(R.id.full_search_results_list);

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                parentFragment.navigateToTitle(((FullSearchResult) searchResultsList.getAdapter().getItem(position)).getTitle());
                dismiss();
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        resultsStatusText = (TextView) dlgLayout.findViewById(R.id.search_results_status);
        searchFailedText = (TextView) dlgLayout.findViewById(R.id.search_failed_text);
        searchFailedContainer = dlgLayout.findViewById(R.id.search_failed_container);

        searchRetryButton = dlgLayout.findViewById(R.id.search_retry_button);
        searchRetryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSearch(FullSearchDialog.this.searchTerm, false, 0);
            }
        });

        searchProgressBar = dlgLayout.findViewById(R.id.search_progress_bar);

        totalResults = new ArrayList<FullSearchResult>();
        adapter.setResults(totalResults);

        doSearch(searchTerm, false, 0);
    }

    private void doSearch(final String searchTerm, final boolean fromSuggestion, final int continueOffset) {
        (new FullSearchArticlesTask(app, app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm, continueOffset) {
            @Override
            public void onFinish(FullSearchResults results) {
                lastResults = results;
                totalResults.addAll(lastResults.getResults());

                ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetChanged();

                searchProgressBar.setVisibility(View.GONE);
                if (lastResults.getResults().size() == 0) {
                    searchResultsContainer.setVisibility(View.GONE);
                    searchFailedContainer.setVisibility(View.VISIBLE);
                    searchFailedText.setText(app.getString(R.string.search_no_results, FullSearchDialog.this.searchTerm));
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
                if (caught instanceof FullSearchSuggestionException) {
                    if (!fromSuggestion) {
                        FullSearchDialog.this.searchTerm = ((FullSearchSuggestionException) caught).getSuggestion();
                        doSearch(FullSearchDialog.this.searchTerm, true, 0);
                    }
                } else {
                    if (continueOffset == 0) {
                        searchProgressBar.setVisibility(View.GONE);
                        searchResultsContainer.setVisibility(View.GONE);
                        searchFailedContainer.setVisibility(View.VISIBLE);
                        searchFailedText.setText(app.getString(R.string.error_network_error));
                        searchRetryButton.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(parentFragment.getActivity(), parentFragment.getString(R.string.error_network_error), Toast.LENGTH_SHORT).show();
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
                doSearch(searchTerm, false, lastResults.getContinueOffset());
            }

            return convertView;
        }
    }

}

