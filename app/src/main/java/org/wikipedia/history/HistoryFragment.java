package org.wikipedia.history;

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.page.PageTitle;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;

import java.text.DateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.Constants.HISTORY_FRAGMENT_LOADER_ID;

public class HistoryFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onLoadPage(PageTitle title, HistoryEntry entry);
        void onClearHistory();
    }

    private Unbinder unbinder;
    @BindView(R.id.history_list) RecyclerView historyList;
    @BindView(R.id.history_empty_container) View historyEmptyView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;

    private WikipediaApp app;

    private String currentSearchQuery;
    private LoaderCallback loaderCallback = new LoaderCallback();
    private HistoryEntryItemAdapter adapter = new HistoryEntryItemAdapter();

    private ItemCallback itemCallback = new ItemCallback();
    private ActionMode actionMode;
    private SearchActionModeCallback searchActionModeCallback = new HistorySearchCallback();

    @NonNull public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        app = WikipediaApp.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        unbinder = ButterKnife.bind(this, view);

        searchEmptyView.setEmptyText(R.string.search_history_no_results);

        historyList.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList.setAdapter(adapter);

        getActivity().getSupportLoaderManager().initLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        getActivity().getSupportLoaderManager().destroyLoader(HISTORY_FRAGMENT_LOADER_ID);
        historyList.setAdapter(null);
        adapter.setCursor(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        if (!isAdded()) {
            return;
        }
        if (!visible && actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionMode != null) {
            actionMode.finish();
            return true;
        }
        return false;
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(View.GONE);
            historyEmptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            searchEmptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            historyEmptyView.setVisibility(View.GONE);
        }
        historyList.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.getRefWatcher().watch(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_history, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isHistoryAvailable = !adapter.isEmpty();
        menu.findItem(R.id.menu_clear_all_history)
                .setVisible(isHistoryAvailable)
                .setEnabled(isHistoryAvailable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_all_history:
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.dialog_title_clear_history)
                        .setMessage(R.string.dialog_message_clear_history)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Clear history!
                                new DeleteAllHistoryTask(app).execute();
                                onClearHistoryClick();
                        }
                        })
                        .setNegativeButton(R.string.no, null).create().show();
                return true;
            case R.id.menu_search_history:
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) getActivity())
                            .startSupportActionMode(searchActionModeCallback);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setActionModeIntTitle(int count, ActionMode mode) {
        mode.setTitle(getString(R.string.multi_select_items_selected, count));
    }

    private void onPageClick(PageTitle title, HistoryEntry entry) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoadPage(title, entry);
        }
    }

    private void onClearHistoryClick() {
        Callback callback = callback();
        if (callback != null) {
            callback.onClearHistory();
        }
    }

    private void restartLoader() {
        getActivity().getSupportLoaderManager().restartLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
    }

    private class LoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String titleCol = PageHistoryContract.PageWithImage.TITLE.qualifiedName();
            String selection = null;
            String[] selectionArgs = null;
            String searchStr = currentSearchQuery;
            if (!TextUtils.isEmpty(searchStr)) {
                searchStr = searchStr.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                selection = "UPPER(" + titleCol + ") LIKE UPPER(?) ESCAPE '\\'";
                selectionArgs = new String[]{"%" + searchStr + "%"};
            }

            Uri uri = PageHistoryContract.PageWithImage.URI;
            final String[] projection = null;
            String order = PageHistoryContract.PageWithImage.ORDER_MRU;
            return new CursorLoader(getContext().getApplicationContext(),
                    uri, projection, selection, selectionArgs, order);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            adapter.setCursor(cursor);
            if (!isAdded()) {
                return;
            }
            updateEmptyState(currentSearchQuery);
            getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.setCursor(null);
        }
    }

    private class HistoryEntryItemHolder extends DefaultViewHolder<PageItemView<HistoryEntry>> {
        private HistoryEntry entry;

        HistoryEntryItemHolder(PageItemView<HistoryEntry> itemView) {
            super(itemView);
        }

        void bindItem(@NonNull Cursor cursor) {
            entry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
            getView().setItem(entry);
            getView().setTitle(entry.getTitle().getDisplayText());
            getView().setDescription(entry.getTitle().getDescription());
            getView().setImageUrl(PageHistoryContract.PageWithImage.IMAGE_NAME.val(cursor));

            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime = getDateString(entry.getTimestamp());
            String prevTime = "";
            if (cursor.getPosition() != 0) {
                cursor.moveToPrevious();
                HistoryEntry prevEntry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
                prevTime = getDateString(prevEntry.getTimestamp());
                cursor.moveToNext();
            }
            getView().setHeaderText(curTime.equals(prevTime) ? null : curTime);
        }

        private String getDateString(Date date) {
            return DateFormat.getDateInstance().format(date);
        }
    }

    private final class HistoryEntryItemAdapter extends RecyclerView.Adapter<HistoryEntryItemHolder> {
        @Nullable private Cursor cursor;

        @Override
        public int getItemCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        public boolean isEmpty() {
            return getItemCount() == 0;
        }

        public void setCursor(@Nullable Cursor newCursor) {
            if (cursor == newCursor) {
                return;
            }
            if (cursor != null) {
                cursor.close();
            }
            cursor = newCursor;
            this.notifyDataSetChanged();
        }

        @Override
        public HistoryEntryItemHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new HistoryEntryItemHolder(new PageItemView<HistoryEntry>(getContext()));
        }

        @Override
        public void onBindViewHolder(HistoryEntryItemHolder holder, int pos) {
            if (cursor == null) {
                return;
            }
            cursor.moveToPosition(pos);
            holder.bindItem(cursor);
        }

        @Override public void onViewAttachedToWindow(HistoryEntryItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(itemCallback);
        }

        @Override public void onViewDetachedFromWindow(HistoryEntryItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ItemCallback implements PageItemView.Callback<HistoryEntry> {
        @Override
        public void onClick(@Nullable HistoryEntry entry) {
            if (entry != null) {
                HistoryEntry newEntry = new HistoryEntry(entry.getTitle(), HistoryEntry.SOURCE_HISTORY);
                onPageClick(entry.getTitle(), newEntry);
            }
        }

        @Override
        public boolean onLongClick(@Nullable HistoryEntry entry) {
            // TODO: multi-select
            return true;
        }

        @Override
        public void onThumbClick(@Nullable HistoryEntry entry) {
            onClick(entry);
        }

        @Override
        public void onActionClick(@Nullable HistoryEntry entry, @NonNull PageItemView view) {
        }
    }

    private class HistorySearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s;
            restartLoader();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                currentSearchQuery = "";
                restartLoader();
            }
            actionMode = null;
        }

        @Override
        protected String getSearchHintString() {
            return getContext().getResources().getString(R.string.search_hint_search_history);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
