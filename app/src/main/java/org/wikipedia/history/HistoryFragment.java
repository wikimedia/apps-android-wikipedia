package org.wikipedia.history;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.lang3.StringUtils;
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
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.WikiCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
    private CompositeDisposable disposables = new CompositeDisposable();
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
        historyEmptyView.setVisibility(View.GONE);

        setUpScrollListener();
        return view;
    }

    private void setUpScrollListener() {
        historyList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                ((MainActivity) requireActivity()).updateToolbarElevation(historyList.computeVerticalScrollOffset() != 0);
            }
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
        reloadHistoryItems();
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
        disposables.clear();
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
            historyEmptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            searchEmptyView.setVisibility(adapter.isEmpty() ? View.VISIBLE : View.GONE);
            historyEmptyView.setVisibility(View.GONE);
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
            reloadHistoryItems();
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
            actionMode.setTitle(getResources().getQuantityString(R.plurals.multi_items_selected, selectedCount, selectedCount));
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
            reloadHistoryItems();
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
            reloadHistoryItems();
        });
        snackbar.show();
    }

    private void reloadHistoryItems() {
        disposables.clear();
        disposables.add(Observable.fromCallable(() -> HistoryDbHelper.INSTANCE.filterHistoryItems(StringUtils.defaultString(currentSearchQuery)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onLoadItemsFinished, t -> {
                    L.e(t);
                    onLoadItemsFinished(Collections.emptyList());
                }));
    }

    private void onLoadItemsFinished(@NonNull List<Object> items) {
        List<Object> list = new ArrayList<>();
        if (!HistorySearchCallback.is(actionMode)) {
            list.add(new SearchBar());
        }
        list.addAll(items);
        adapter.setList(list);
        updateEmptyState(currentSearchQuery);
        requireActivity().invalidateOptionsMenu();
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

    private static class HeaderViewHolder extends DefaultViewHolder<View> {
        TextView headerText;
        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.section_header_text);
        }

        void bindItem(@NonNull String date) {
            headerText.setText(date);
        }
    }

    private class SearchCardViewHolder extends DefaultViewHolder<View> {
        private ImageView historyFilterButton;
        private ImageView clearHistoryButton;
        private WikiCardView searchWikiCardView;

        SearchCardViewHolder(View itemView) {
            super(itemView);
            searchWikiCardView = itemView.findViewById(R.id.wiki_card_for_search);
            View searchCardView = itemView.findViewById(R.id.search_card);
            AppCompatImageView voiceSearchButton = itemView.findViewById(R.id.voice_search_button);
            historyFilterButton = itemView.findViewById(R.id.history_filter);
            clearHistoryButton = itemView.findViewById(R.id.history_delete);
            searchCardView.setOnClickListener(view -> ((MainFragment) getParentFragment()).openSearchActivity(Constants.InvokeSource.NAV_MENU, null, searchWikiCardView));
            voiceSearchButton.setOnClickListener(view -> ((MainFragment) getParentFragment()).onFeedVoiceSearchRequested());
            historyFilterButton.setOnClickListener(view -> {
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) requireActivity())
                            .startSupportActionMode(searchActionModeCallback);
                }
            });
            clearHistoryButton.setOnClickListener(view -> {
                if (selectedEntries.size() == 0) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_clear_history)
                            .setMessage(R.string.dialog_message_clear_history)
                            .setPositiveButton(R.string.dialog_message_clear_history_yes, (dialog, which) -> onClearHistoryClick())
                            .setNegativeButton(R.string.dialog_message_clear_history_no, null).create().show();
                } else {
                    deleteSelectedPages();
                }
            });
            FeedbackUtil.setButtonLongPressToast(historyFilterButton, clearHistoryButton);
            searchWikiCardView.setCardBackgroundColor(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_22));
        }

        public void bindItem() {
            clearHistoryButton.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
            historyFilterButton.setVisibility(adapter.isEmpty() ? View.GONE : View.VISIBLE);
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

        private static final int VIEW_TYPE_SEARCH_CARD = 0;
        private static final int VIEW_TYPE_HEADER = 1;
        private static final int VIEW_TYPE_ITEM = 2;

        @NonNull
        private List<Object> historyEntries = new ArrayList<>();

        @Override
        public int getItemCount() {
            return historyEntries.size();
        }

        public boolean isEmpty() {
            return (getItemCount() == 0)
                    || (getItemCount() == 1 && historyEntries.get(0) instanceof SearchBar);
        }

        @Override
        public int getItemViewType(int position) {
            if (historyEntries.get(position) instanceof SearchBar) {
                return VIEW_TYPE_SEARCH_CARD;
            } else if (historyEntries.get(position) instanceof String) {
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
            if (viewType == VIEW_TYPE_SEARCH_CARD) {
                View view = LayoutInflater.from(requireContext()).inflate(R.layout.view_history_header_with_search, parent, false);
                return new SearchCardViewHolder(view);
            } else if (viewType == VIEW_TYPE_HEADER) {
                View view = LayoutInflater.from(requireContext()).inflate(R.layout.view_section_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                return new HistoryEntryItemHolder(new PageItemView<>(requireContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            if (holder instanceof SearchCardViewHolder) {
                ((SearchCardViewHolder) holder).bindItem();
            } else if (holder instanceof HistoryEntryItemHolder) {
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

        public void hideHeader() {
            if (!historyEntries.isEmpty() && historyEntries.get(0) instanceof SearchBar) {
                historyEntries.remove(0);
                notifyDataSetChanged();
            }
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
            actionMode = mode;
            ((MainFragment) getParentFragment()).setBottomNavVisible(false);
            ((HistoryEntryItemAdapter)historyList.getAdapter()).hideHeader();
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s.trim();
            reloadHistoryItems();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            currentSearchQuery = "";
            reloadHistoryItems();
            actionMode = null;
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

    private static class SearchBar { }
}
