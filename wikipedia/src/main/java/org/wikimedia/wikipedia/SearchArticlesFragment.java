package org.wikimedia.wikipedia;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.squareup.picasso.Picasso;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;

import java.util.List;
import java.util.Map;

public class SearchArticlesFragment extends Fragment {
    private static final int DELAY_MILLIS = 300;

    private static final int MESSAGE_SEARCH = 1;

    private WikipediaApp app;
    private EditText searchTermText;
    private ListView searchResultsList;
    private ProgressBar searchProgress;

    private SearchResultAdapter adapter;

    private boolean isSearchActive = false;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache = new ParcelableLruCache<List<PageTitle>>(4, List.class);
    private ParcelableLruCache<String> pageImagesCache = new ParcelableLruCache<String>(48, String.class);
    private String lastSearchedText;

    private Handler searchHandler;

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private void displayResults(List<PageTitle> results) {
        adapter.setResults(results);
        ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
        if (adapter.getCount() == 0) {
            searchResultsList.setVisibility(View.GONE);
            isSearchActive = false;
            // Stupid android, making me hide the keyboard manually
            InputMethodManager inputManager = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
            getActivity().getCurrentFocus().clearFocus();
        } else {
            searchResultsList.setVisibility(View.VISIBLE);
            PageImagesTask imagesTask = new PageImagesTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), results, 48) {
                @Override
                public void onFinish(Map<PageTitle, String> result) {
                    for(Map.Entry<PageTitle, String> entry : result.entrySet()) {
                        if (entry.getValue() == null) {
                            continue;
                        }
                        pageImagesCache.put(entry.getKey().getPrefixedText(), entry.getValue());
                    }
                    ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
                }
            };
            imagesTask.execute();
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (WikipediaApp)getActivity().getApplicationContext();
        LinearLayout parentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_search, container, false);

        if (savedInstanceState != null) {
            searchResultsCache = savedInstanceState.getParcelable("searchResultsCache");
            pageImagesCache = savedInstanceState.getParcelable("pageImagesCache");
            lastSearchedText = savedInstanceState.getString("lastSearchedText");
            isSearchActive = savedInstanceState.getBoolean("isSearchActive");
        }

        searchTermText = (EditText) parentLayout.findViewById(R.id.searchTermText);
        searchResultsList = (ListView) parentLayout.findViewById(R.id.searchResultsList);
        searchProgress = (ProgressBar) parentLayout.findViewById(R.id.searchProgress);

        searchHandler = new Handler(new Handler.Callback(){
            @Override
            public boolean handleMessage(Message msg) {
                final String searchTerm = (String) msg.obj;
                Log.d("Wikipedia", "Searching for " + searchTerm);
                SearchArticlesTask searchTask = new SearchArticlesTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm) {
                    @Override
                    public void onFinish(List<PageTitle> result) {
                        searchProgress.setVisibility(View.GONE);
                        displayResults(result);
                        searchResultsCache.put(searchTerm, result);
                        lastSearchedText = searchTerm;
                    }

                    @Override
                    public void onBeforeExecute() {
                        searchProgress.setVisibility(View.VISIBLE);
                        isSearchActive = true;
                    }
                };
                searchTask.execute();
                return true;
            }
        });

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) searchResultsList.getAdapter().getItem(position);
                app.getBus().post(new NewWikiPageNavigationEvent(title));
                displayResults(null);
            }
        });

        searchTermText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // DO NOTHING!
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // DO NOTHING
            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (Utils.compareStrings(s.toString(), lastSearchedText) && !isSearchActive) {
                    return; // Nothing has changed!
                }
                if (s.toString().equals("")) {
                	return; // nothing!
                }

                List<PageTitle> cacheResult = searchResultsCache.get(s.toString());
                if (cacheResult != null) {
                    displayResults(cacheResult);
                    return;
                }
                searchHandler.removeMessages(MESSAGE_SEARCH);
                Message searchMessage = Message.obtain();
                searchMessage.what = MESSAGE_SEARCH;
                searchMessage.obj = s.toString();

                searchHandler.sendMessageDelayed(searchMessage, DELAY_MILLIS);
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        return parentLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("searchResultsCache", searchResultsCache);
        outState.putParcelable("pageImagesCache", pageImagesCache);
        outState.putString("lastSearchedText", lastSearchedText);
        outState.putBoolean("isSearchActive", isSearchActive);
    }

    private class SearchResultAdapter extends BaseAdapter {
        private List<PageTitle> results;
        private final LayoutInflater inflater;

        private SearchResultAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        private List<PageTitle> getResults() {
            return results;
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
            pageTitleText.setText(title.getText());
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

    /**
     * Handle back button being pressed.
     *
     * @return true if the back button press was handled, false otherwise
     */
    public boolean handleBackPressed() {
        if (isSearchActive) {
            displayResults(null);
            return true;
        }
        return false;
    }
}
