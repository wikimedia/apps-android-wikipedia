package org.wikipedia.readinglist;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.main.MainActivity;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.TextInputDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.readinglist.ReadingListActivity.EXTRA_READING_LIST_TITLE;
import static org.wikipedia.readinglist.ReadingLists.SORT_BY_NAME_ASC;

public class ReadingListFragment extends Fragment {
    @BindView(R.id.reading_list_toolbar) Toolbar toolbar;
    @BindView(R.id.reading_list_toolbar_container) CollapsingToolbarLayout toolBarLayout;
    @BindView(R.id.reading_list_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.reading_list_header) ReadingListHeaderView headerImageView;
    @BindView(R.id.reading_list_contents) RecyclerView recyclerView;
    @BindView(R.id.reading_list_empty_text) TextView emptyView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    private Unbinder unbinder;

    @Nullable private ReadingList readingList;
    private ReadingListPageItemAdapter adapter = new ReadingListPageItemAdapter();
    private ReadingListItemView headerView;
    @Nullable private ActionMode actionMode;
    private AppBarListener appBarListener = new AppBarListener();
    private boolean showOverflowMenu;

    @NonNull private ReadingLists readingLists = new ReadingLists();
    private ReadingListsFunnel funnel = new ReadingListsFunnel();
    private HeaderCallback headerCallback = new HeaderCallback();
    private ItemCallback itemCallback = new ItemCallback();
    private SearchCallback searchActionModeCallback = new SearchCallback();
    private MultiSelectActionModeCallback multiSelectActionModeCallback = new MultiSelectCallback();

    @NonNull private List<ReadingListPage> displayedPages = new ArrayList<>();
    private String currentSearchQuery;

    @NonNull
    public static ReadingListFragment newInstance(@NonNull String listTitle) {
        ReadingListFragment instance = new ReadingListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_READING_LIST_TITLE, listTitle);
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_reading_list, container, false);
        unbinder = ButterKnife.bind(this, view);

        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        appBarLayout.addOnOffsetChangedListener(appBarListener);
        toolBarLayout.setCollapsedTitleTextColor(Color.WHITE);

        ItemTouchHelper.Callback touchCallback = new ReadingListItemTouchHelperCallback(getContext());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));

        headerView = new ReadingListItemView(getContext());
        headerView.setCallback(headerCallback);
        headerView.setClickable(false);
        headerView.setThumbnailVisible(false);
        headerView.setShowDescriptionEmptyHint(true);
        headerView.setTitleTextSize(R.dimen.readingListTitleTextSize);

        final String readingListTitle = getArguments().getString(EXTRA_READING_LIST_TITLE);
        ReadingList.DAO.queryMruLists(null, new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> lists) {
                if (getActivity() == null) {
                    return;
                }
                readingLists.set(lists);
                readingList = readingLists.get(readingListTitle);
                if (readingList != null) {
                    searchEmptyView.setEmptyText(getString(R.string.search_reading_list_no_results,
                            readingList.getTitle()));
                }
                update();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override public void onDestroyView() {
        readingList = null;
        readingLists.set(Collections.<ReadingList>emptyList());
        recyclerView.setAdapter(null);
        appBarLayout.removeOnOffsetChangedListener(appBarListener);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_reading_lists, menu);
        if (showOverflowMenu) {
            inflater.inflate(R.menu.menu_reading_list_item, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem sortByNameItem = menu.findItem(R.id.menu_sort_by_name);
        MenuItem sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent);
        int sortMode = Prefs.getReadingListPageSortMode(ReadingLists.SORT_BY_NAME_ASC);
        sortByNameItem.setTitle(sortMode == ReadingLists.SORT_BY_NAME_ASC ? R.string.reading_list_sort_by_name_desc : R.string.reading_list_sort_by_name);
        sortByRecentItem.setTitle(sortMode == ReadingLists.SORT_BY_RECENT_DESC ? R.string.reading_list_sort_by_recent_desc : R.string.reading_list_sort_by_recent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_lists:
                getAppCompatActivity().startSupportActionMode(searchActionModeCallback);
                return true;
            case R.id.menu_sort_by_name:
                setSortMode(ReadingLists.SORT_BY_NAME_ASC, ReadingLists.SORT_BY_NAME_DESC);
                return true;
            case R.id.menu_sort_by_recent:
                setSortMode(ReadingLists.SORT_BY_RECENT_DESC, ReadingLists.SORT_BY_RECENT_ASC);
                return true;
            case R.id.menu_reading_list_rename:
                rename();
                return true;
            case R.id.menu_reading_list_edit_description:
                editDescription();
                return true;
            case R.id.menu_reading_list_delete:
                delete();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void update() {
        if (readingList == null) {
            return;
        }
        emptyView.setVisibility(readingList.getPages().isEmpty() ? View.VISIBLE : View.GONE);
        headerView.setReadingList(readingList);
        headerImageView.setReadingList(readingList);
        readingList.sort(Prefs.getReadingListPageSortMode(SORT_BY_NAME_ASC));
        setSearchQuery(currentSearchQuery);
    }

    private void setSearchQuery(@Nullable String query) {
        if (readingList == null) {
            return;
        }
        currentSearchQuery = query;
        displayedPages.clear();
        if (TextUtils.isEmpty(query)) {
            displayedPages.addAll(readingList.getPages());
        } else {
            query = query.toUpperCase();
            for (ReadingListPage page : readingList.getPages()) {
                if (page.title().toUpperCase().contains(query.toUpperCase())) {
                    displayedPages.add(page);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState(query);
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(View.GONE);
            emptyView.setVisibility(displayedPages.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            searchEmptyView.setVisibility(displayedPages.isEmpty() ? View.VISIBLE : View.GONE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void setSortMode(int sortModeAsc, int sortModeDesc) {
        int sortMode = Prefs.getReadingListPageSortMode(ReadingLists.SORT_BY_NAME_ASC);
        if (sortMode != sortModeAsc) {
            sortMode = sortModeAsc;
        } else {
            sortMode = sortModeDesc;
        }
        Prefs.setReadingListPageSortMode(sortMode);
        getActivity().supportInvalidateOptionsMenu();
        update();
    }

    private void showDeleteItemsUndoSnackbar(final ReadingList readingList, final List<ReadingListPage> pages) {
        String message = pages.size() == 1
                ? String.format(getString(R.string.reading_list_item_deleted), pages.get(0).title())
                : String.format(getString(R.string.reading_list_items_deleted), pages.size());
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(), message,
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (ReadingListPage page : pages) {
                    ReadingList.DAO.addTitleToList(readingList, page);
                    ReadingListPageDao.instance().markOutdated(page);
                }
                update();
            }
        });
        snackbar.show();
    }

    private void rename() {
        if (readingList == null) {
            return;
        }
        ReadingListTitleDialog.readingListTitleDialog(getContext(), readingList.getTitle(),
                readingLists.getTitlesExcept(readingList.getTitle()),
                new ReadingListTitleDialog.Callback() {
                    @Override
                    public void onSuccess(@NonNull CharSequence text) {
                        ReadingList.DAO.renameAndSaveListInfo(readingList, text.toString());
                        update();
                        funnel.logModifyList(readingList, readingLists.size());
                    }
                }).show();
    }

    private void editDescription() {
        if (readingList == null) {
            return;
        }
        TextInputDialog.newInstance(getContext(), new TextInputDialog.DefaultCallback() {
            @Override
            public void onShow(@NonNull TextInputDialog dialog) {
                dialog.setHint(R.string.reading_list_description_hint);
                dialog.setText(readingList.getDescription());
            }

            @Override
            public void onSuccess(@NonNull CharSequence text) {
                readingList.setDescription(text.toString());
                ReadingList.DAO.saveListInfo(readingList);
                update();
                funnel.logModifyList(readingList, readingLists.size());
            }
        }).show();
    }

    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void beginMultiSelect() {
        if (SearchCallback.is(actionMode)) {
            finishActionMode();
        }
        if (!MultiSelectCallback.is(actionMode)) {
            getAppCompatActivity().startSupportActionMode(multiSelectActionModeCallback);
        }
    }

    private void toggleSelectPage(@Nullable ReadingListPage page) {
        if (page == null) {
            return;
        }
        page.setSelected(!page.isSelected());
        int selectedCount = getSelectedPageCount();
        if (selectedCount == 0) {
            finishActionMode();
        } else if (actionMode != null) {
            actionMode.setTitle(getString(R.string.multi_select_items_selected, selectedCount));
        }
        adapter.notifyDataSetChanged();
    }

    private int getSelectedPageCount() {
        int selectedCount = 0;
        for (ReadingListPage page : displayedPages) {
            if (page.isSelected()) {
                selectedCount++;
            }
        }
        return selectedCount;
    }

    private void unselectAllPages() {
        if (readingList == null) {
            return;
        }
        for (ReadingListPage page : readingList.getPages()) {
            page.setSelected(false);
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteSelectedPages() {
        if (readingList == null) {
            return;
        }
        List<ReadingListPage> selectedPages = new ArrayList<>();
        for (ReadingListPage page : displayedPages) {
            if (page.isSelected()) {
                selectedPages.add(page);
                page.setSelected(false);
                ReadingList.DAO.removeTitleFromList(readingList, page);
            }
        }
        if (!selectedPages.isEmpty()) {
            funnel.logDeleteItem(readingList, readingLists.size());
            showDeleteItemsUndoSnackbar(readingList, selectedPages);
            update();
        }
    }

    private void delete() {
        if (readingList != null) {
            startActivity(MainActivity.newIntent(getContext())
                    .putExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST, readingList.getTitle()));
            getActivity().finish();
        }
    }

    private class AppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            if (verticalOffset > -appBarLayout.getTotalScrollRange() && showOverflowMenu) {
                showOverflowMenu = false;
                toolBarLayout.setTitle("");
                getAppCompatActivity().supportInvalidateOptionsMenu();
            } else if (verticalOffset <= -appBarLayout.getTotalScrollRange() && !showOverflowMenu) {
                showOverflowMenu = true;
                toolBarLayout.setTitle(readingList.getTitle());
                getAppCompatActivity().supportInvalidateOptionsMenu();
            }
        }
    }

    private class HeaderCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
        }

        @Override
        public void onRename(@NonNull final ReadingList readingList) {
            rename();
        }

        @Override
        public void onEditDescription(@NonNull final ReadingList readingList) {
            editDescription();
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
            delete();
        }
    }

    class ReadingListPageItemHolder extends DefaultViewHolder<PageItemView<ReadingListPage>>
                implements ReadingListItemTouchHelperCallback.Callback {
        private ReadingListPage page;

        ReadingListPageItemHolder(PageItemView<ReadingListPage> itemView) {
            super(itemView);
        }

        void bindItem(ReadingListPage page) {
            this.page = page;
            getView().setItem(page);
            getView().setActionIcon(R.drawable.ic_offline_pin_black_24dp);
            getView().setTitle(page.title());
            getView().setDescription(page.description());
            getView().setImageUrl(page.thumbnailUrl());
            getView().setSelected(page.isSelected());
        }

        @Override
        public void onDismiss() {
            if (readingList != null) {
                showDeleteItemsUndoSnackbar(readingList, Collections.singletonList(page));
                ReadingList.DAO.removeTitleFromList(readingList, page);
                funnel.logDeleteItem(readingList, readingLists.size());
                update();
            }
        }
    }

    private class ReadingListHeaderHolder extends RecyclerView.ViewHolder {
        ReadingListHeaderHolder(View itemView) {
            super(itemView);
        }
    }

    private final class ReadingListPageItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        @Override
        public int getItemCount() {
            return 1 + displayedPages.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            if (type == TYPE_HEADER) {
                return new ReadingListHeaderHolder(headerView);
            }
            return new ReadingListPageItemHolder(new PageItemView<ReadingListPage>(getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
            if (readingList != null && holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).bindItem(displayedPages.get(pos - 1));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).getView().setCallback(itemCallback);
            }
        }

        @Override public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            if (holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).getView().setCallback(null);
            }
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ItemCallback implements PageItemView.Callback<ReadingListPage> {
        @Override
        public void onClick(@Nullable ReadingListPage page) {
            if (MultiSelectCallback.is(actionMode)) {
                toggleSelectPage(page);
            } else if (page != null && readingList != null) {
                PageTitle title = ReadingListDaoProxy.pageTitle(page);
                HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);
                ReadingList.DAO.makeListMostRecent(readingList);
                startActivity(PageActivity.newIntent(getContext(), entry, entry.getTitle()));
            }
        }

        @Override
        public boolean onLongClick(@Nullable ReadingListPage item) {
            beginMultiSelect();
            toggleSelectPage(item);
            return true;
        }

        @Override
        public void onThumbClick(@Nullable ReadingListPage item) {
            onClick(item);
        }

        @Override
        public void onActionClick(@Nullable ReadingListPage item) {
            // TODO
        }
    }

    private class SearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            setSearchQuery(s);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            actionMode = null;
            setSearchQuery(null);
        }

        @Override
        protected String getSearchHintString() {
            return getString(R.string.search_hint_search_reading_list);
        }
    }

    private class MultiSelectCallback extends MultiSelectActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onDelete() {
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
}
