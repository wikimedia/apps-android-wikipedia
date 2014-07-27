package org.wikipedia.search;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import org.wikipedia.PageTitle;
import org.wikipedia.ParcelableLruCache;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikipedia.events.ShowToCEvent;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActionsHandler;
import org.wikipedia.page.PopupMenu;
import org.wikipedia.pageimages.PageImagesTask;

import java.util.List;
import java.util.Map;

public class SearchArticlesFragment extends Fragment {
    private static final int DELAY_MILLIS = 300;
    private static final int MAX_CACHE_SIZE_SEARCH_RESULTS = 4;
    private static final int MAX_CACHE_SIZE_IMAGES = 48;
    private static final float THUMB_SIZE_DP = 48f;
    private static final int LEFT_MARGIN_BASE_DP = 10;
    private static final float INITIAL_OFFSET_WINDOW = 0.5f;
    private static final int MESSAGE_SEARCH = 1;

    private WikipediaApp app;

    private EditText searchTermText;
    private ListView searchResultsList;
    private ProgressBar searchProgress;
    private View searchNetworkError;
    private View searchNoResults;
    private ImageView searchBarMenuButton;
    private ImageView searchBarTocButton;
    private ImageView drawerIndicator;

    private SearchResultAdapter adapter;

    private boolean isSearchActive = false;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache
            = new ParcelableLruCache<List<PageTitle>>(MAX_CACHE_SIZE_SEARCH_RESULTS, List.class);
    private ParcelableLruCache<String> pageImagesCache
            = new ParcelableLruCache<String>(MAX_CACHE_SIZE_IMAGES, String.class);
    private String lastSearchedText;

    private Handler searchHandler;

    private DrawerLayout drawerLayout;

    private PageActionsHandler pageActionsHandler;
    private PopupMenu pageActionsMenu;

    private SearchArticlesTask curSearchTask;

    private boolean pausedStateOfZero;

    private void hideSearchResults() {
        searchResultsList.setVisibility(View.GONE);
        isSearchActive = false;
        searchHandler.removeMessages(MESSAGE_SEARCH);
        if (curSearchTask != null) {
            curSearchTask.cancel();
            curSearchTask = null;
        }
        searchProgress.setVisibility(View.INVISIBLE);
        searchNoResults.setVisibility(View.GONE);
    }

    /**
     * Displays results passed to it as search suggestions.
     *
     * @param results List of results to display. If null, clears the list of suggestions & hides it.
     */
    private void displayResults(List<PageTitle> results) {
        adapter.setResults(results);
        ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
        if (results.size() == 0) {
            searchNoResults.setVisibility(View.VISIBLE);
        } else {
            searchResultsList.setVisibility(View.VISIBLE);
            PageImagesTask imagesTask = new PageImagesTask(
                    app.getAPIForSite(app.getPrimarySite()),
                    app.getPrimarySite(),
                    results,
                    (int)(THUMB_SIZE_DP * WikipediaApp.SCREEN_DENSITY)) {
                @Override
                public void onFinish(Map<PageTitle, String> result) {
                    for (Map.Entry<PageTitle, String> entry : result.entrySet()) {
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
            pausedStateOfZero = savedInstanceState.getBoolean("pausedStateOfZero");
        }

        searchTermText = (EditText) parentLayout.findViewById(R.id.search_term_text);
        searchResultsList = (ListView) parentLayout.findViewById(R.id.search_results_list);
        searchProgress = (ProgressBar) parentLayout.findViewById(R.id.search_progress);
        View searchBarIcon = parentLayout.findViewById(R.id.search_bar_icon);
        searchNetworkError = parentLayout.findViewById(R.id.search_network_error);
        searchBarMenuButton = (ImageView)parentLayout.findViewById(R.id.search_bar_show_menu);
        searchBarTocButton = (ImageView)parentLayout.findViewById(R.id.search_bar_show_toc);
        drawerIndicator = (ImageView)parentLayout.findViewById(R.id.search_drawer_indicator);
        ImageView wikipediaIcon = (ImageView) parentLayout.findViewById(R.id.wikipedia_icon);
        searchNoResults = parentLayout.findViewById(R.id.search_results_empty);

        app.adjustDrawableToTheme(wikipediaIcon.getDrawable());
        app.adjustDrawableToTheme(searchBarTocButton.getDrawable());
        app.adjustDrawableToTheme(searchBarMenuButton.getDrawable());

        pageActionsMenu = new PopupMenu(getActivity(), searchBarMenuButton);

        searchHandler = new Handler(new SearchHandlerCallback());

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) searchResultsList.getAdapter().getItem(position);
                navigateToTitle(title);
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
                searchNoResults.setVisibility(View.GONE);
                startSearch(s.toString());
            }
        });

        searchTermText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START);
                }
                Utils.showSoftKeyboardAsync(getActivity(), searchTermText);
                startSearch(searchTermText.getText().toString());
            }
        });

        searchTermText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (action == EditorInfo.IME_ACTION_GO) {
                    String searchTerm = searchTermText.getText().toString();
                    if (searchTerm.length() > 0) {
                        PageTitle title = new PageTitle(searchTermText.getText().toString(), app.getPrimarySite());
                        navigateToTitle(title);
                    } else {
                        hideSearchResults();
                    }
                    return true;
                }
                return false;
            }
        });

        searchTermText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus && searchNetworkError.isShown()) {
                    ViewAnimations.fadeOut(searchNetworkError);
                }
                if (hasFocus) {
                    if (drawerLayout.isDrawerOpen(Gravity.START)) {
                        drawerLayout.closeDrawer(Gravity.START);
                    }
                    Utils.showSoftKeyboardAsync(getActivity(), searchTermText);
                } else {
                    Utils.hideSoftKeyboard(getActivity());
                }
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
                    hideSearchResults();
                    clearErrors();
                    drawerLayout.openDrawer(Gravity.START);
                }
            }
        });

        searchBarTocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawerLayout.isDrawerVisible(Gravity.START)) {
                    drawerLayout.closeDrawer(Gravity.START);
                }
                Utils.hideSoftKeyboard(getActivity());
                app.getBus().post(new ShowToCEvent());
            }
        });

        return parentLayout;
    }

    private void navigateToTitle(PageTitle title) {
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
        Utils.hideSoftKeyboard(getActivity());
        app.getBus().post(new NewWikiPageNavigationEvent(title, historyEntry));
        hideSearchResults();
        // remove focus from the Search field, so that the cursor stops blinking
        searchTermText.clearFocus();
    }

    private void startSearch(String term) {
        if (Utils.compareStrings(term, lastSearchedText) && isSearchActive) {
            return; // Nothing has changed!
        }
        if (term.equals("")) {
            hideSearchResults();
            return;
        }

        List<PageTitle> cacheResult = searchResultsCache.get(app.getPrimaryLanguage() + "-" + term);
        if (cacheResult != null) {
            displayResults(cacheResult);
            isSearchActive = true;
            return;
        }
        searchHandler.removeMessages(MESSAGE_SEARCH);
        Message searchMessage = Message.obtain();
        searchMessage.what = MESSAGE_SEARCH;
        searchMessage.obj = term;

        searchHandler.sendMessageDelayed(searchMessage, DELAY_MILLIS);
    }

    public void ensureVisible() {
        ViewAnimations.ensureTranslationY(getView(), 0);
    }

    public void setDrawerLayout(DrawerLayout drawerLayout) {
        this.drawerLayout = drawerLayout;

        if (pageActionsHandler != null) {
            pageActionsHandler.onDestroy();
        }
        pageActionsHandler = new PageActionsHandler(app.getBus(), pageActionsMenu, searchBarMenuButton, drawerLayout);

        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            private boolean hideKeyboardCalled = false;
            private float offsetWindow = INITIAL_OFFSET_WINDOW;
            private float offsetWindowMax = offsetWindow;
            private float offsetWindowMin = 0f;

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Hide the keyboard when the drawer is opened
                if (!hideKeyboardCalled) {
                    Utils.hideSoftKeyboard(getActivity());
                    hideKeyboardCalled = true;
                }
                // Make sure that the entire search bar is visible
                ensureVisible();
                // Animation for sliding drawer open and close
                // Modeled after the general behavior of Google apps
                // Shift left and right margins appropriately to make sure that the rest of the layout does not shift
                if (slideOffset >= offsetWindowMax) {
                    offsetWindowMax = slideOffset;
                    offsetWindowMin = offsetWindowMax - offsetWindow;
                } else if (slideOffset < offsetWindowMin) {
                    offsetWindowMin = slideOffset;
                    offsetWindowMax = offsetWindowMin + offsetWindow;
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(drawerIndicator.getLayoutParams());
                params.leftMargin = -(int)(LEFT_MARGIN_BASE_DP * WikipediaApp.SCREEN_DENSITY * offsetWindowMin);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                        && drawerView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    params.leftMargin = -params.leftMargin;
                }
                params.rightMargin = -params.leftMargin;
                params.gravity = Gravity.CENTER_VERTICAL; // Needed because this seems to get reset otherwise.
                drawerIndicator.setLayoutParams(params);
            }
            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_IDLE) {
                    hideKeyboardCalled = false;
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("searchResultsCache", searchResultsCache);
        outState.putParcelable("pageImagesCache", pageImagesCache);
        outState.putString("lastSearchedText", lastSearchedText);
        outState.putBoolean("isSearchActive", isSearchActive);
        outState.putBoolean("pausedStateOfZero", pausedStateOfZero);
    }

    public void setTocEnabled(boolean enable) {
        searchBarTocButton.setEnabled(enable);
    }

    public void setTocHidden(boolean hide) {
        searchBarTocButton.setVisibility(hide ? View.GONE : View.VISIBLE);
    }

    public void clearErrors() {
        searchNetworkError.setVisibility(View.GONE);
    }

    private final class SearchResultAdapter extends BaseAdapter {
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
            pageTitleText.setText(title.getDisplayText());
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
            hideSearchResults();
            return true;
        }
        return false;
    }

    @Subscribe
    public void onNewWikiPageNavigationEvent(NewWikiPageNavigationEvent event) {
        if (event.getHistoryEntry().getSource() != HistoryEntry.SOURCE_SEARCH) {
            // Clear navigation text if we used something other than search to go to the new page
            searchTermText.setText("");
        }
        // If search bar isn't fully visible, make it so!
        ensureVisible();
    }

    @Subscribe
    public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
        if (WikipediaApp.getWikipediaZeroDisposition()) {
            setWikipediaZeroChrome();
        } else {
            setNormalChrome();
        }
    }

    private void setWikipediaZeroChrome() {
        //navbar.setBackgroundColor(Color.BLACK);
        //drawerIndicator.setColorFilter(Color.WHITE);
        //wikipediaIcon.setColorFilter(Color.WHITE);
        //searchTermText.setTextColor(Color.WHITE);
        searchTermText.setHint(R.string.zero_search_hint);
        //searchBarMenuButton.setColorFilter(Color.WHITE);
        //searchBarTocButton.setColorFilter(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //drawerIndicator.setAlpha(.6f);
            //wikipediaIcon.setAlpha(.6f);
            //searchBarTocButton.setAlpha(.6f);
        }
        ensureVisible();
    }

    private void setNormalChrome() {
        //navbar.setBackgroundColor(navbarColor);
        //drawerIndicator.clearColorFilter();
        //wikipediaIcon.clearColorFilter();
        //searchTermText.setTextColor(searchTermTextColor);
        //searchTermText.setHintTextColor(searchTermHintTextColor);
        searchTermText.setHint(R.string.search_hint);
        //searchBarMenuButton.clearColorFilter();
        //searchBarTocButton.clearColorFilter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //drawerIndicator.setAlpha(1.0f);
            //wikipediaIcon.setAlpha(.9f);
            //searchBarTocButton.setAlpha(.5f);
        }
        ensureVisible();
    }

    @Override
    public void onPause() {
        super.onPause();
        pausedStateOfZero = WikipediaApp.getWikipediaZeroDisposition();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean latestWikipediaZeroDispostion = WikipediaApp.getWikipediaZeroDisposition();
        if (pausedStateOfZero != latestWikipediaZeroDispostion) {
            app.getBus().post(new WikipediaZeroStateChangeEvent());
        } else if (latestWikipediaZeroDispostion) {
            setWikipediaZeroChrome();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        app.getBus().register(this);
    }

    @Override
    public void onDestroyView() {
        app.getBus().unregister(this);
        if (pageActionsHandler != null) {
            pageActionsHandler.onDestroy();
        }
        super.onDestroyView();
    }

    private class SearchHandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            final String searchTerm = (String) msg.obj;
            SearchArticlesTask searchTask = new SearchArticlesTask(app, app.getAPIForSite(app.getPrimarySite()), app.getPrimarySite(), searchTerm) {
                @Override
                public void onFinish(List<PageTitle> result) {
                    searchProgress.setVisibility(View.INVISIBLE);
                    searchNetworkError.setVisibility(View.GONE);
                    displayResults(result);
                    searchResultsCache.put(app.getPrimaryLanguage() + "-" + searchTerm, result);
                    lastSearchedText = searchTerm;
                    curSearchTask = null;
                }

                @Override
                public void onCatch(Throwable caught) {
                    searchProgress.setVisibility(View.INVISIBLE);
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
    }
}
