package org.wikimedia.wikipedia;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;

import java.util.List;

public class SearchArticlesFragment extends Fragment {
    private WikipediaApp app;
    private EditText searchTermText;
    private ListView searchResultsList;
    private ProgressBar searchProgress;

    private SearchResultAdapter adapter;

    private SearchArticlesTask currentTask;
    private boolean isSearchActive = false;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache = new ParcelableLruCache<List<PageTitle>>(4, List.class);
    private String lastSearchedText;

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
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (WikipediaApp)getActivity().getApplicationContext();
        LinearLayout parentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_search, container, false);

        if (savedInstanceState != null) {
            searchResultsCache = savedInstanceState.getParcelable("searchResultsCache");
            lastSearchedText = savedInstanceState.getString("lastSearchedText");
            isSearchActive = savedInstanceState.getBoolean("isSearchActive");
        }

        searchTermText = (EditText) parentLayout.findViewById(R.id.searchTermText);
        searchResultsList = (ListView) parentLayout.findViewById(R.id.searchResultsList);
        searchProgress = (ProgressBar) parentLayout.findViewById(R.id.searchProgress);

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
                if (currentTask != null) {
                    currentTask.cancel();
                }

                List<PageTitle> cacheResult = searchResultsCache.get(s.toString());
                if (cacheResult != null) {
                    displayResults(cacheResult);
                    return;
                }
                SearchArticlesTask searchTask = new SearchArticlesTask(getActivity(), app.getPrimarySite(), s.toString()) {
                    @Override
                    public void onFinish(List<PageTitle> result) {
                        displayResults(result);
                        searchProgress.setVisibility(View.GONE);
                        searchResultsCache.put(s.toString(), result);
                        lastSearchedText = s.toString();
                    }

                    @Override
                    public void onBeforeExecute() {
                        searchProgress.setVisibility(View.VISIBLE);
                        isSearchActive = true;
                    }
                };
                if (currentTask != null) {
                    currentTask.cancel();
                }
                searchTask.execute();
                currentTask = searchTask;
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
        outState.putString("lastSearchedText", lastSearchedText);
        outState.putBoolean("isSearchActive", isSearchActive);
    }

    private static class SearchResultAdapter extends BaseAdapter {
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

            return convertView;
        }
    }
}
