package org.wikipedia.history;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

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
    private MultiSelectCallback multiSelectCallback = new MultiSelectCallback();
    private HashSet<Integer> selectedIndices = new HashSet<>();

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

        ItemTouchHelper.Callback touchCallback = new SwipeableItemTouchHelperCallback(getContext());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(historyList);

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
                        .setPositiveButton(R.string.dialog_message_clear_history_yes, (dialog, which) -> {
                            // Clear history!
                            new DeleteAllHistoryTask(app).execute();
                            onClearHistoryClick();
                        })
                        .setNegativeButton(R.string.dialog_message_clear_history_no, null).create().show();
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

    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void beginMultiSelect() {
        if (HistorySearchCallback.is(actionMode)) {
            finishActionMode();
        }
        if (!MultiSelectCallback.is(actionMode)) {
            ((AppCompatActivity) getActivity()).startSupportActionMode(multiSelectCallback);
        }
    }

    private void toggleSelectPage(@Nullable IndexedHistoryEntry indexedEntry) {
        if (indexedEntry == null) {
            return;
        }
        if (selectedIndices.contains(indexedEntry.getIndex())) {
            selectedIndices.remove(indexedEntry.getIndex());
        } else {
            selectedIndices.add(indexedEntry.getIndex());
        }
        int selectedCount = selectedIndices.size();
        if (selectedCount == 0) {
            finishActionMode();
        } else if (actionMode != null) {
            actionMode.setTitle(getString(R.string.multi_select_items_selected, selectedCount));
        }
        adapter.notifyDataSetChanged();
    }

    private void unselectAllPages() {
        selectedIndices.clear();
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedPages() {
        List<HistoryEntry> selectedEntries = new ArrayList<>();
        for (int index : selectedIndices) {
            HistoryEntry entry = adapter.getItem(index);
            if (entry != null) {
                selectedEntries.add(entry);
                app.getDatabaseClient(HistoryEntry.class).delete(entry,
                        PageHistoryContract.PageWithImage.SELECTION);
            }
        }
        selectedIndices.clear();
        if (!selectedEntries.isEmpty()) {
            showDeleteItemsUndoSnackbar(selectedEntries);
            adapter.notifyDataSetChanged();
        }
    }

    private void showDeleteItemsUndoSnackbar(final List<HistoryEntry> entries) {
        String message = entries.size() == 1
                ? String.format(getString(R.string.history_item_deleted), entries.get(0).getTitle().getDisplayText())
                : String.format(getString(R.string.history_items_deleted), entries.size());
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(), message,
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.history_item_delete_undo, (v) -> {
            DatabaseClient<HistoryEntry> client = app.getDatabaseClient(HistoryEntry.class);
            for (HistoryEntry entry : entries) {
                client.upsert(entry, PageHistoryContract.PageWithImage.SELECTION);
            }
            adapter.notifyDataSetChanged();
        });
        snackbar.show();
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
            getActivity().invalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            adapter.setCursor(null);
        }
    }

    private static class IndexedHistoryEntry {
        private final int index;
        @NonNull private final HistoryEntry entry;

        IndexedHistoryEntry(@NonNull HistoryEntry entry, int index) {
            this.entry = entry;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @NonNull public HistoryEntry getEntry() {
            return entry;
        }
    }

    private class HistoryEntryItemHolder extends DefaultViewHolder<PageItemView<IndexedHistoryEntry>>
            implements SwipeableItemTouchHelperCallback.Callback {
        private int index;

        HistoryEntryItemHolder(PageItemView<IndexedHistoryEntry> itemView) {
            super(itemView);
        }

        void bindItem(@NonNull Cursor cursor) {
            index = cursor.getPosition();
            IndexedHistoryEntry indexedEntry
                    = new IndexedHistoryEntry(HistoryEntry.DATABASE_TABLE.fromCursor(cursor), index);
            getView().setItem(indexedEntry);
            getView().setTitle(indexedEntry.getEntry().getTitle().getDisplayText());
            getView().setDescription(indexedEntry.getEntry().getTitle().getDescription());
            getView().setImageUrl(PageHistoryContract.PageWithImage.IMAGE_NAME.val(cursor));
            getView().setSelected(selectedIndices.contains(indexedEntry.getIndex()));

            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it this is the first item.
            String curTime = getDateString(indexedEntry.getEntry().getTimestamp());
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

        @Override
        public void onSwipe() {
            selectedIndices.add(index);
            deleteSelectedPages();
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

        @Nullable public HistoryEntry getItem(int position) {
            if (cursor == null) {
                return null;
            }
            int prevPosition = cursor.getPosition();
            cursor.moveToPosition(position);
            HistoryEntry entry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
            cursor.moveToPosition(prevPosition);
            return entry;
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
            return new HistoryEntryItemHolder(new PageItemView<IndexedHistoryEntry>(getContext()));
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

    private class ItemCallback implements PageItemView.Callback<IndexedHistoryEntry> {
        @Override
        public void onClick(@Nullable IndexedHistoryEntry indexedEntry) {
            if (MultiSelectCallback.is(actionMode)) {
                toggleSelectPage(indexedEntry);
            } else if (indexedEntry != null) {
                HistoryEntry newEntry = new HistoryEntry(indexedEntry.getEntry().getTitle(), HistoryEntry.SOURCE_HISTORY);
                onPageClick(indexedEntry.getEntry().getTitle(), newEntry);
            }
        }

        @Override
        public boolean onLongClick(@Nullable IndexedHistoryEntry indexedEntry) {
            beginMultiSelect();
            toggleSelectPage(indexedEntry);
            return true;
        }

        @Override
        public void onThumbClick(@Nullable IndexedHistoryEntry indexedEntry) {
            onClick(indexedEntry);
        }

        @Override
        public void onActionClick(@Nullable IndexedHistoryEntry entry, @NonNull View view) {
        }
        @Override
        public void onSecondaryActionClick(@Nullable IndexedHistoryEntry entry, @NonNull View view) {
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
            currentSearchQuery = s.trim();
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

    private class MultiSelectCallback extends MultiSelectActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            super.onCreateActionMode(mode, menu);
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_history, menu);
            actionMode = mode;
            selectedIndices.clear();
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onDeleteSelected() {
            deleteSelectedPages();
            finishActionMode();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            unselectAllPages();
            actionMode = null;
            super.onDestroyActionMode(mode);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
