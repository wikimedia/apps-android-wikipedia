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

import java.util.ArrayList;
import java.util.List;

public class SearchArticlesFragment extends Fragment {
    private WikipediaApp app;
    private EditText searchTermText;
    private ListView searchResultsList;
    private ProgressBar searchProgress;

    private List<PageTitle> currentResults = new ArrayList<PageTitle>();

    private SearchArticlesTask currentTask;

    private ParcelableLruCache<List<PageTitle>> searchResultsCache = new ParcelableLruCache<List<PageTitle>>(4, List.class);

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        app = (WikipediaApp)getActivity().getApplicationContext();
        LinearLayout parentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_search, container, false);

        if (savedInstanceState != null) {
            searchResultsCache = savedInstanceState.getParcelable("searchResultsCache");
        }

        searchTermText = (EditText) parentLayout.findViewById(R.id.searchTermText);
        searchResultsList = (ListView) parentLayout.findViewById(R.id.searchResultsList);
        searchProgress = (ProgressBar) parentLayout.findViewById(R.id.searchProgress);

        searchTermText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                app.getBus().post(new NewWikiPageNavigationEvent(new PageTitle(null, searchTermText.getText().toString(), app.getPrimarySite())));
                return true;
            }
        });

        searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageTitle title = (PageTitle) searchResultsList.getAdapter().getItem(position);
                app.getBus().post(new NewWikiPageNavigationEvent(title));
                currentResults.clear();
                ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
                // Stupid android, making me hide the keyboard manually
                InputMethodManager inputManager = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
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
                if (currentTask != null) {
                    currentTask.cancel();
                }

                List<PageTitle> cacheResult = searchResultsCache.get(s.toString());
                if (cacheResult != null) {
                    currentResults.clear();
                    currentResults.addAll(cacheResult);
                    ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
                    return;
                }
                SearchArticlesTask searchTask = new SearchArticlesTask(getActivity(), app.getPrimarySite(), s.toString()) {
                    @Override
                    public void onFinish(List<PageTitle> result) {
                        currentResults.clear();
                        currentResults.addAll(result);
                        ((BaseAdapter)searchResultsList.getAdapter()).notifyDataSetInvalidated();
                        searchProgress.setVisibility(View.GONE);
                        searchResultsCache.put(s.toString(), result);
                    }

                    @Override
                    public void onBeforeExecute() {
                        searchProgress.setVisibility(View.VISIBLE);
                    }
                };
                if (currentTask != null) {
                    currentTask.cancel();
                }
                searchTask.execute();
                currentTask = searchTask;
            }
        });

        searchResultsList.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return currentResults == null ? 0 : currentResults.size();
            }

            @Override
            public Object getItem(int position) {
                return currentResults.get(position);
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
        });

        return parentLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("searchResultsCache", searchResultsCache);
    }
}
