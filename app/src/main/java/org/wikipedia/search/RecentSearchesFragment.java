package org.wikipedia.search;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.database.contract.SearchHistoryContract;
import org.wikipedia.util.FeedbackUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.wikipedia.Constants.RECENT_SEARCHES_FRAGMENT_LOADER_ID;

/** Displays a list of recent searches */
public class RecentSearchesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public interface Parent {
        void switchToSearch(@NonNull String text);
    }

    private Parent parentFragment;
    private RecentSearchesAdapter adapter;

    @BindView(R.id.recent_searches_list) ListView recentSearchesList;
    @BindView(R.id.search_empty_container) View searchEmptyView;
    @BindView(R.id.recent_searches_container) View recentSearchesContainer;
    @BindView(R.id.recent_searches) View recentSearches;
    @BindView(R.id.recent_searches_delete_button) ImageView deleteButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_recent, container, false);
        ButterKnife.bind(this, rootView);

        parentFragment = (Parent) getParentFragment();
        deleteButton.setOnClickListener((view) -> {
            new AlertDialog.Builder(getContext())
                    .setMessage(getString(R.string.clear_recent_searches_confirm))
                    .setPositiveButton(
                            getString(R.string.clear_recent_searches_confirm_yes), (dialog, id) -> new DeleteAllRecentSearchesTask(WikipediaApp.getInstance()).execute())
                    .setNegativeButton(getString(R.string.clear_recent_searches_confirm_no), null)
                    .create().show();
        });
        FeedbackUtil.setToolbarButtonLongPressToast(deleteButton);

        return rootView;
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
            parentFragment.switchToSearch(entry.getText());
        });

        LoaderManager supportLoaderManager = getLoaderManager();
        supportLoaderManager.initLoader(RECENT_SEARCHES_FRAGMENT_LOADER_ID, null, this);
        supportLoaderManager.restartLoader(RECENT_SEARCHES_FRAGMENT_LOADER_ID, null, this);
    }

    @Override
    public void onDestroyView() {
        getLoaderManager().destroyLoader(RECENT_SEARCHES_FRAGMENT_LOADER_ID);
        super.onDestroyView();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Uri uri = SearchHistoryContract.Query.URI;
        final String[] projection = null;
        final String selection = null;
        final String[] selectionArgs = null;
        String order = SearchHistoryContract.Query.ORDER_MRU;
        return new CursorLoader(getContext(), uri, projection, selection, selectionArgs, order);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoaderLoader, Cursor cursorLoader) {
        if (!isAdded()) {
            return;
        }
        adapter.swapCursor(cursorLoader);
        boolean searchesEmpty = recentSearchesList.getCount() == 0;
        searchEmptyView.setVisibility(searchesEmpty ? View.VISIBLE : View.INVISIBLE);
        recentSearches.setVisibility(!searchesEmpty ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoaderLoader) {
        adapter.changeCursor(null);
    }

    public void updateList() {
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
            TextView textView = view.findViewById(R.id.text1);
            RecentSearch entry = getEntry(cursor);
            textView.setText(entry.getText());
            view.setTag(entry);
        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            return getEntry(cursor).getText();
        }

        public RecentSearch getEntry(Cursor cursor) {
            return RecentSearch.DATABASE_TABLE.fromCursor(cursor);
        }
    }
}
