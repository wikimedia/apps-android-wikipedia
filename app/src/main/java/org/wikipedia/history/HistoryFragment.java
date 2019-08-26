package org.wikipedia.history;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.main.MainFragment;
import org.wikipedia.page.PageAvailableOfflineHandler;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.ViewUtil;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.HISTORY_FRAGMENT_LOADER_ID;

public class HistoryFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onLoadPage(@NonNull HistoryEntry entry);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        unbinder = ButterKnife.bind(this, view);

        searchEmptyView.setEmptyText(R.string.search_history_no_results);

        SwipeableItemTouchHelperCallback touchCallback = new SwipeableItemTouchHelperCallback(requireContext());
        touchCallback.setSwipeableEnabled(true);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(historyList);

        historyList.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList.setAdapter(adapter);

        LoaderManager.getInstance(requireActivity()).initLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEmptyState();
    }

    @Override
    public void onDestroyView() {
        LoaderManager.getInstance(requireActivity()).destroyLoader(HISTORY_FRAGMENT_LOADER_ID);
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

    private void updateEmptyState() {
        updateEmptyState(null);
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(View.GONE);
            setEmptyContainerVisibility(adapter.isEmpty());
        } else {
            searchEmptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            setEmptyContainerVisibility(false);
        }
        historyList.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setEmptyContainerVisibility(boolean visible) {
        if (visible) {
            historyEmptyView.setVisibility(View.VISIBLE);
            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        } else {
            historyEmptyView.setVisibility(View.GONE);
            DeviceUtil.setWindowSoftInputModeResizable(requireActivity());
        }
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
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.dialog_title_clear_history)
                        .setMessage(R.string.dialog_message_clear_history)
                        .setPositiveButton(R.string.dialog_message_clear_history_yes, (dialog, which) -> {
                            // Clear history!
                            Completable.fromAction(() -> app.getDatabaseClient(HistoryEntry.class).deleteAll())
                                    .subscribeOn(Schedulers.io()).subscribe();
                            onClearHistoryClick();
                        })
                        .setNegativeButton(R.string.dialog_message_clear_history_no, null).create().show();
                return true;
            case R.id.menu_search_history:
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) requireActivity())
                            .startSupportActionMode(searchActionModeCallback);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onPageClick(@NonNull HistoryEntry entry) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoadPage(entry);
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
            ((AppCompatActivity) requireActivity()).startSupportActionMode(multiSelectCallback);
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

    public void refresh() {
        adapter.notifyDataSetChanged();
        if (!app.isOnline() && Prefs.shouldShowHistoryOfflineArticlesToast()) {
            Toast.makeText(requireContext(), R.string.history_offline_articles_toast, Toast.LENGTH_SHORT).show();
            Prefs.shouldShowHistoryOfflineArticlesToast(false);
        }
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
        LoaderManager.getInstance(requireActivity()).restartLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
    }

    private class LoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        @NonNull @Override
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
            String order = PageHistoryContract.PageWithImage.ORDER_MRU;
            return new CursorLoader(requireContext().getApplicationContext(), uri, null, selection, selectionArgs, order);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
            adapter.setCursor(cursor);
            if (!isAdded()) {
                return;
            }
            updateEmptyState(currentSearchQuery);
            requireActivity().invalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
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
            PageAvailableOfflineHandler.INSTANCE.check(indexedEntry.getEntry().getTitle(), available -> getView().setViewsGreyedOut(!available));

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
        public HistoryEntryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new HistoryEntryItemHolder(new PageItemView<>(requireContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryEntryItemHolder holder, int pos) {
            if (cursor == null) {
                return;
            }
            cursor.moveToPosition(pos);
            holder.bindItem(cursor);
        }

        @Override public void onViewAttachedToWindow(@NonNull HistoryEntryItemHolder holder) {
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
                onPageClick(new HistoryEntry(indexedEntry.getEntry().getTitle(), HistoryEntry.SOURCE_HISTORY));
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

        @Override
        public void onListChipClick(@NonNull ReadingList readingList) {
        }
    }

    private class HistorySearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            ViewUtil.finishActionModeWhenTappingOnView(getView(), actionMode);
            ViewUtil.finishActionModeWhenTappingOnView(historyList, actionMode);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s.trim();
            ((MainFragment) getParentFragment())
                    .setBottomNavVisible(currentSearchQuery.length() == 0);
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
            return requireContext().getResources().getString(R.string.search_hint_search_history);
        }

        @Override
        protected boolean finishActionModeIfKeyboardHiding() {
            return true;
        }

        @Override
        protected Context getParentContext() {
            return requireContext();
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
