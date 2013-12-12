package org.wikimedia.wikipedia;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.squareup.picasso.Picasso;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikimedia.wikipedia.history.HistoryEntry;
import org.wikimedia.wikipedia.pageimages.PageImagesTask;

import java.util.List;
import java.util.Map;

public class SearchArticlesFragment extends Fragment {
    private static final int DELAY_MILLIS = 300;

    private static final int MESSAGE_SEARCH = 1;

    private WikipediaApp app;

    private ImageView searchBarIcon;
    private EditText searchTermText;
    private ListView searchResultsList;
    private ProgressBar searchProgress;
    private View searchNetworkError;
    private View searchBarMenuButton;

    private SearchResultAdapter adapter;

    private boolean isSearchActive = false;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache = new ParcelableLruCache<List<PageTitle>>(4, List.class);
    private ParcelableLruCache<String> pageImagesCache = new ParcelableLruCache<String>(48, String.class);
    private String lastSearchedText;

    private Handler searchHandler;

    private DrawerLayout drawerLayout;

    private SearchArticlesTask curSearchTask;

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
            PageImagesTask imagesTask = new PageImagesTask(
                    app.getAPIForSite(app.getPrimarySite()),
                    app.getPrimarySite(),
                    results,
                    (int)(48f * WikipediaApp.SCREEN_DENSITY)) {
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

                @Override
                public void onCatch(Throwable caught) {
                    // Don't actually do anything.
                    // Thumbnails are expendable
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
        searchBarIcon = (ImageView) parentLayout.findViewById(R.id.searchBarIcon);
        searchNetworkError = parentLayout.findViewById(R.id.searchNetworkError);
        searchBarMenuButton = parentLayout.findViewById(R.id.searchBarShowMenu);

        PopupMenu pageActionsMenu = new PopupMenu(getActivity(), searchBarMenuButton);
        PageActionsHandler pageActionsHandler = new PageActionsHandler(app.getBus(), pageActionsMenu, searchBarMenuButton);

        searchHandler = new Handler(new Handler.Callback(){
            @Override
            public boolean handleMessage(Message msg) {
                final String searchTerm = (String) msg.obj;
                Log.d("Wikipedia", "Searching for " + searchTerm);
                SearchArticlesTask searchTask = new SearchArticlesTask(app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm) {
                    @Override
                    public void onFinish(List<PageTitle> result) {
                        searchProgress.setVisibility(View.GONE);
                        searchNetworkError.setVisibility(View.GONE);
                        displayResults(result);
                        searchResultsCache.put(searchTerm, result);
                        lastSearchedText = searchTerm;
                        curSearchTask = null;
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        searchProgress.setVisibility(View.GONE);
                        searchNetworkError.setVisibility(View.VISIBLE);
                        searchResultsList.setVisibility(View.GONE);
                        curSearchTask = null;
                    }

                    @Override
                    public void onBeforeExecute() {
                        searchProgress.setVisibility(View.VISIBLE);
                        isSearchActive = true;
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
        });

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) searchResultsList.getAdapter().getItem(position);
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
                app.getBus().post(new NewWikiPageNavigationEvent(title, historyEntry));
                displayResults(null);
            }
        });

        searchNetworkError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Just retry!
                startSearch(searchTermText.getText().toString());
                searchNetworkError.setVisibility(View.GONE);
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
                startSearch(s.toString());
            }
        });

        adapter = new SearchResultAdapter(inflater);
        searchResultsList.setAdapter(adapter);

        searchBarIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerVisible(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START);
                } else {
                    drawerLayout.openDrawer(Gravity.START);
                }
            }
        });

        return parentLayout;
    }

    private void startSearch(String term) {
        if (Utils.compareStrings(term, lastSearchedText) && !isSearchActive) {
            return; // Nothing has changed!
        }
        if (term.equals("")) {
            return; // nothing!
        }

        List<PageTitle> cacheResult = searchResultsCache.get(term);
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

    void setDrawerLayout(DrawerLayout drawerLayout) {
        this.drawerLayout = drawerLayout;
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
