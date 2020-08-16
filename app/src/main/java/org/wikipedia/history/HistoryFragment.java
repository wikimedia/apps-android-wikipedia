package org.wikipedia.history;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.database.DatabaseClient;
import org.wikipedia.database.contract.PageHistoryContract;
import org.wikipedia.main.MainActivity;
import org.wikipedia.main.MainFragment;
import org.wikipedia.page.PageAvailableOfflineHandler;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.WikiCardView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.wikipedia.Constants.HISTORY_FRAGMENT_LOADER_ID;

public class HistoryFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onLoadPage(@NonNull HistoryEntry entry);
        void onClearHistory();
    }

    private Unbinder unbinder;
    @BindView(R.id.history_scroll_view) NestedScrollView historyNestedScrollView;
    @BindView(R.id.history_container) View historyContainer;
    @BindView(R.id.history_list) RecyclerView historyList;
    @BindView(R.id.history_empty_container) View historyEmptyView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.history_delete) ImageView deleteHistoryItems;
    @BindView(R.id.history_filter) ImageView filterHistoryItems;
    @BindView(R.id.wiki_card_for_search) WikiCardView searchWikiCardView;

    private WikipediaApp app;

    private String currentSearchQuery;
    private LoaderCallback loaderCallback = new LoaderCallback();
    private HistoryEntryItemAdapter adapter = new HistoryEntryItemAdapter();

    private ItemCallback itemCallback = new ItemCallback();
    private ActionMode actionMode;
    private SearchActionModeCallback searchActionModeCallback = new HistorySearchCallback();
    private HashSet<HistoryEntry> selectedEntries = new HashSet<>();
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
        searchWikiCardView.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_22));

        LoaderManager.getInstance(requireActivity()).initLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
        setUpScrollListener();
        return view;
    }

    @OnClick(R.id.history_delete) void onDeleteHistoryEntriesClicked(View v) {
        if (selectedEntries.size() == 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_title_clear_history)
                    .setMessage(R.string.dialog_message_clear_history)
                    .setPositiveButton(R.string.dialog_message_clear_history_yes, (dialog, which) -> onClearHistoryClick())
                    .setNegativeButton(R.string.dialog_message_clear_history_no, null).create().show();
        } else {
            deleteSelectedPages();
        }
    }

    @OnClick(R.id.history_filter) void onFilterEntriesClicked(View v) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) requireActivity())
                    .startSupportActionMode(searchActionModeCallback);
        }
    }

    @OnClick(R.id.search_card) void onSearchCardClicked(View v) {
        ((MainFragment) getParentFragment()).openSearchActivity(Constants.InvokeSource.NAV_MENU, null);
    }

    private void setUpScrollListener() {
        historyNestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            ((MainActivity) requireActivity()).updateToolbarElevation(scrollY != 0);
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        restartLoader();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onDestroyView() {
        LoaderManager.getInstance(requireActivity()).destroyLoader(HISTORY_FRAGMENT_LOADER_ID);
        historyList.setAdapter(null);
        historyList.clearOnScrollListeners();
        adapter.clearList();
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public boolean onBackPressed() {
        if (actionMode != null) {
            actionMode.finish();
            return true;
        }
        if (selectedEntries.size() > 0) {
            unselectAllPages();
            return true;
        }
        return false;
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
        deleteHistoryItems.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
        filterHistoryItems.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
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
            restartLoader();
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
    }

    private void toggleSelectPage(@Nullable IndexedHistoryEntry indexedEntry) {
        if (indexedEntry == null) {
            return;
        }
        if (selectedEntries.contains(indexedEntry.getEntry())) {
            selectedEntries.remove(indexedEntry.getEntry());
        } else {
            selectedEntries.add(indexedEntry.getEntry());
        }
        int selectedCount = selectedEntries.size();
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
        selectedEntries.clear();
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedPages() {
        List<HistoryEntry> selectedEntryList = new ArrayList<>();
        for (HistoryEntry entry : selectedEntries) {
            if (entry != null) {
                selectedEntryList.add(entry);
                app.getDatabaseClient(HistoryEntry.class).delete(entry,
                        PageHistoryContract.PageWithImage.SELECTION);
            }
        }
        selectedEntries.clear();
        if (!selectedEntryList.isEmpty()) {
            showDeleteItemsUndoSnackbar(selectedEntryList);
            restartLoader();
        }
    }

    private void showDeleteItemsUndoSnackbar(final List<HistoryEntry> entries) {
        String message = entries.size() == 1
                ? getString(R.string.history_item_deleted, entries.get(0).getTitle().getDisplayText())
                : getString(R.string.history_items_deleted, entries.size());
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(), message,
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.history_item_delete_undo, (v) -> {
            DatabaseClient<HistoryEntry> client = app.getDatabaseClient(HistoryEntry.class);
            for (HistoryEntry entry : entries) {
                client.upsert(entry, PageHistoryContract.PageWithImage.SELECTION);
            }
            restartLoader();
        });
        snackbar.show();
    }

    private void restartLoader() {
        LoaderManager.getInstance(requireActivity()).restartLoader(HISTORY_FRAGMENT_LOADER_ID, null, loaderCallback);
    }

    private class LoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        @NonNull @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String titleCol = PageHistoryContract.PageWithImage.API_TITLE.qualifiedName();
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
            List<Object> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                IndexedHistoryEntry indexedEntry = new IndexedHistoryEntry(cursor);
                // Check the previous item, see if the times differ enough
                // If they do, display the section header.
                // Always do it if this is the first item.
                String curTime = getDateString(indexedEntry.getEntry().getTimestamp());
                String prevTime;
                if (cursor.getPosition() != 0) {
                    cursor.moveToPrevious();
                    HistoryEntry prevEntry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
                    prevTime = getDateString(prevEntry.getTimestamp());
                    if (!curTime.equals(prevTime)) {
                        list.add(curTime);
                    }
                    cursor.moveToNext();
                } else {
                    list.add(curTime);
                }
                list.add(indexedEntry); //add the item
            }
            adapter.setList(list);
            cursor.close();

            if (!isAdded()) {
                return;
            }

            updateEmptyState(currentSearchQuery);
            requireActivity().invalidateOptionsMenu();
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            adapter.clearList();
        }

        private String getDateString(Date date) {
            return DateFormat.getDateInstance().format(date);
        }
    }

    public static class IndexedHistoryEntry {
        @NonNull private final HistoryEntry entry;
        @Nullable private final String imageUrl;

        public IndexedHistoryEntry(@NonNull Cursor cursor) {
            this.entry = HistoryEntry.DATABASE_TABLE.fromCursor(cursor);
            this.imageUrl = PageHistoryContract.PageWithImage.IMAGE_NAME.val(cursor);
        }

        @Nullable public String getImageUrl() {
            return imageUrl;
        }

        @NonNull public HistoryEntry getEntry() {
            return entry;
        }
    }

    private class HeaderViewHolder extends DefaultViewHolder<View> {
        TextView headerText;
        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.section_header_text);
        }

        void bindItem(@NonNull String date) {
            headerText.setText(date);
        }
    }

    private class HistoryEntryItemHolder extends DefaultViewHolder<PageItemView<IndexedHistoryEntry>>
            implements SwipeableItemTouchHelperCallback.Callback {
        private HistoryEntry entry;

        HistoryEntryItemHolder(PageItemView<IndexedHistoryEntry> itemView) {
            super(itemView);
        }

        void bindItem(@NonNull IndexedHistoryEntry indexedEntry) {
            entry = indexedEntry.getEntry();
            getView().setItem(indexedEntry);
            getView().setTitle(indexedEntry.getEntry().getTitle().getDisplayText());
            getView().setDescription(indexedEntry.getEntry().getTitle().getDescription());
            getView().setImageUrl(indexedEntry.getImageUrl());
            getView().setSelected(selectedEntries.contains(indexedEntry.getEntry()));
            PageAvailableOfflineHandler.INSTANCE.check(indexedEntry.getEntry().getTitle(), available -> getView().setViewsGreyedOut(!available));
        }


        @Override
        public void onSwipe() {
            selectedEntries.add(entry);
            deleteSelectedPages();
        }
    }

    private final class HistoryEntryItemAdapter extends RecyclerView.Adapter<DefaultViewHolder> {

        private static final int VIEW_TYPE_HEADER = 0;
        private static final int VIEW_TYPE_ITEM = 1;

        @NonNull
        private List<Object> historyEntries = new ArrayList<>();

        @Override
        public int getItemCount() {
            return historyEntries.size();
        }

        public boolean isEmpty() {
            return getItemCount() == 0;
        }

        @Override
        public int getItemViewType(int position) {
            if (historyEntries.get(position) instanceof String) {
                return VIEW_TYPE_HEADER;
            } else {
                return VIEW_TYPE_ITEM;
            }
        }

        public void setList(@NonNull List<Object> list) {
            historyEntries = list;
            this.notifyDataSetChanged();
        }

        void clearList() {
            historyEntries.clear();
        }

        @Override
        public DefaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_HEADER) {
                View view = LayoutInflater.from(requireContext()).inflate(R.layout.view_section_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                return new HistoryEntryItemHolder(new PageItemView<>(requireContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            if (holder instanceof HistoryEntryItemHolder) {
                ((HistoryEntryItemHolder) holder).bindItem((IndexedHistoryEntry) historyEntries.get(pos));
            } else {
                ((HeaderViewHolder) holder).bindItem((String) historyEntries.get(pos));
            }
        }

        @Override public void onViewAttachedToWindow(@NonNull DefaultViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof HistoryEntryItemHolder) {
                ((HistoryEntryItemHolder) holder).getView().setCallback(itemCallback);
            }
        }

        @Override public void onViewDetachedFromWindow(@NonNull DefaultViewHolder holder) {
            if (holder instanceof HistoryEntryItemHolder) {
                ((HistoryEntryItemHolder) holder).getView().setCallback(null);
            }
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ItemCallback implements PageItemView.Callback<IndexedHistoryEntry> {
        @Override
        public void onClick(@Nullable IndexedHistoryEntry indexedEntry) {
            if (selectedEntries != null && !selectedEntries.isEmpty()) {
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
            searchWikiCardView.setVisibility(View.GONE);
            actionMode = mode;
            ((MainFragment) getParentFragment()).setBottomNavVisible(false);
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
            searchWikiCardView.setVisibility(View.VISIBLE);
            ((MainFragment) getParentFragment()).setBottomNavVisible(true);
        }

        @Override
        protected String getSearchHintString() {
            return requireContext().getResources().getString(R.string.history_filter_list_hint);
        }

        @Override
        protected Context getParentContext() {
            return requireContext();
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    public void show() {
        historyContainer.setVisibility(View.VISIBLE);
    }

    public void hide() {
        historyContainer.setVisibility(View.GONE);
    }
}
