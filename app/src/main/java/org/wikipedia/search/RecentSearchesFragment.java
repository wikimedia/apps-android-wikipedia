package org.wikipedia.search;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.databinding.FragmentSearchRecentBinding;
import org.wikipedia.util.FeedbackUtil;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.RECENT_SEARCHES_FRAGMENT_LOADER_ID;

/** Displays a list of recent searches */
public class RecentSearchesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public interface Callback {
        void switchToSearch(@NonNull String text);

        void onAddLanguageClicked();
    }

    private Callback callback;
    private RecentSearchesAdapter adapter;

    private FragmentSearchRecentBinding binding;
    private ListView recentSearchesList;
    private View searchEmptyView;
    private View recentSearchesContainer;
    private View recentSearches;
    private TextView addLanguagesButton;
    private TextView emptyViewMessage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchRecentBinding.inflate(inflater, container, false);

        recentSearchesList = binding.recentSearchesList;
        searchEmptyView = binding.searchEmptyContainer;
        recentSearchesContainer = binding.recentSearchesContainer;
        recentSearches = binding.recentSearches;
        final ImageView deleteButton = binding.recentSearchesDeleteButton;
        addLanguagesButton = binding.addLanguagesButton;
        emptyViewMessage = binding.searchEmptyMessage;

        addLanguagesButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onAddLanguageClicked();
            }
        });

        deleteButton.setOnClickListener((view) ->
                new AlertDialog.Builder(requireContext())
                        .setMessage(getString(R.string.clear_recent_searches_confirm))
                        .setPositiveButton(getString(R.string.clear_recent_searches_confirm_yes), (dialog, id) ->
                                Completable.fromAction(() -> WikipediaApp.getInstance().getDatabaseClient(RecentSearch.class).deleteAll()).subscribeOn(Schedulers.io()).subscribe())
                        .setNegativeButton(getString(R.string.clear_recent_searches_confirm_no), null)
                        .create().show());
        FeedbackUtil.setToolbarButtonLongPressToast(deleteButton);

        return binding.getRoot();
    }

    public void show() {
        recentSearchesContainer.setVisibility(View.VISIBLE);
    }

    public void hide() {
        recentSearchesContainer.setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new RecentSearchesAdapter(getContext(), null, true);
        recentSearchesList.setAdapter(adapter);

        recentSearchesList.setOnItemClickListener((parent, view, position, id) -> {
            RecentSearch entry = (RecentSearch) view.getTag();
            if (callback != null) {
                callback.switchToSearch(entry.getText());
            }
        });

        LoaderManager supportLoaderManager = LoaderManager.getInstance(this);
        supportLoaderManager.initLoader(RECENT_SEARCHES_FRAGMENT_LOADER_ID, null, this);
        supportLoaderManager.restartLoader(RECENT_SEARCHES_FRAGMENT_LOADER_ID, null, this);
    }

    @Override
    public void onDestroyView() {
        LoaderManager.getInstance(this).destroyLoader(RECENT_SEARCHES_FRAGMENT_LOADER_ID);
        binding = null;
        super.onDestroyView();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = SearchHistoryContract.Query.URI;
        String order = SearchHistoryContract.Query.ORDER_MRU;
        return new CursorLoader(requireContext(), uri, null, null, null, order);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoaderLoader, Cursor cursorLoader) {
        if (!isAdded()) {
            return;
        }
        adapter.swapCursor(cursorLoader);
        boolean searchesEmpty = recentSearchesList.getCount() == 0;
        searchEmptyView.setVisibility(searchesEmpty ? View.VISIBLE : View.INVISIBLE);
        updateSearchEmptyView(searchesEmpty);
        recentSearches.setVisibility(!searchesEmpty ? View.VISIBLE : View.INVISIBLE);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void updateSearchEmptyView(boolean searchesEmpty) {
        if (searchesEmpty) {
            searchEmptyView.setVisibility(View.VISIBLE);
            if (WikipediaApp.getInstance().language().getAppLanguageCodes().size() == 1) {
                addLanguagesButton.setVisibility(View.VISIBLE);
                emptyViewMessage.setText(getString(R.string.search_empty_message_multilingual_upgrade));
            } else {
                addLanguagesButton.setVisibility(View.GONE);
                emptyViewMessage.setText(getString(R.string.search_empty_message));
            }
        } else {
            searchEmptyView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoaderLoader) {
        adapter.changeCursor(null);
    }

    void updateList() {
        adapter.notifyDataSetChanged();
    }

    private class RecentSearchesAdapter extends CursorAdapter {
        RecentSearchesAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(getActivity()).inflate(R.layout.item_search_recent, viewGroup, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView textView = (TextView) view;
            RecentSearch entry = getEntry(cursor);
            textView.setText(entry.getText());
            view.setTag(entry);
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            return getEntry(cursor).getText();
        }

        RecentSearch getEntry(Cursor cursor) {
            return RecentSearch.DATABASE_TABLE.fromCursor(cursor);
        }
    }
}
