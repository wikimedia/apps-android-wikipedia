package org.wikipedia.readinglist;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.events.PageDownloadEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.main.MainActivity;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageAvailableOfflineHandler;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.readinglist.sync.ReadingListSyncEvent;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.wikipedia.Constants.InvokeSource.READING_LIST_ACTIVITY;
import static org.wikipedia.readinglist.ReadingListActivity.EXTRA_READING_LIST_ID;
import static org.wikipedia.readinglist.ReadingListsFragment.ARTICLE_ITEM_IMAGE_DIMENSION;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.views.CircularProgressBar.MAX_PROGRESS;

public class ReadingListFragment extends Fragment implements ReadingListItemActionsDialog.Callback {
    @BindView(R.id.reading_list_toolbar) Toolbar toolbar;
    @BindView(R.id.reading_list_toolbar_container) CollapsingToolbarLayout toolBarLayout;
    @BindView(R.id.reading_list_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.reading_list_header) ReadingListHeaderView headerImageView;
    @BindView(R.id.reading_list_contents) RecyclerView recyclerView;
    @BindView(R.id.reading_list_empty_text) TextView emptyView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.reading_list_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable private ReadingList readingList;
    private long readingListId;

    private ReadingListPageItemAdapter adapter = new ReadingListPageItemAdapter();
    private ReadingListItemView headerView;
    @Nullable private ActionMode actionMode;
    private AppBarListener appBarListener = new AppBarListener();
    private boolean showOverflowMenu;

    private ReadingListsFunnel funnel = new ReadingListsFunnel();
    private HeaderCallback headerCallback = new HeaderCallback();
    private ReadingListItemCallback readingListItemCallback = new ReadingListItemCallback();
    private ReadingListPageItemCallback readingListPageItemCallback = new ReadingListPageItemCallback();
    private SearchCallback searchActionModeCallback = new SearchCallback();
    private MultiSelectActionModeCallback multiSelectActionModeCallback = new MultiSelectCallback();
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private SwipeableItemTouchHelperCallback touchCallback;
    private boolean toolbarExpanded = true;

    private List<Object> displayedLists = new ArrayList<>();
    private String currentSearchQuery;
    private boolean articleLimitMessageShown = false;

    @NonNull
    public static ReadingListFragment newInstance(long listId) {
        ReadingListFragment instance = new ReadingListFragment();
        Bundle args = new Bundle();
        args.putLong(EXTRA_READING_LIST_ID, listId);
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_reading_list, container, false);
        unbinder = ButterKnife.bind(this, view);

        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        DeviceUtil.updateStatusBarTheme(requireActivity(), toolbar, true);
        appBarLayout.addOnOffsetChangedListener(appBarListener);
        toolBarLayout.setCollapsedTitleTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.main_toolbar_icon_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            toolBarLayout.setStatusBarScrimColor(ResourceUtil.getThemedColor(requireContext(), R.attr.main_status_bar_color));
        }

        touchCallback = new SwipeableItemTouchHelperCallback(requireContext());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.addItemDecoration(new DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, false));

        headerView = new ReadingListItemView(getContext());
        headerView.setCallback(headerCallback);
        headerView.setClickable(false);
        headerView.setThumbnailVisible(false);
        headerView.setTitleTextAppearance(R.style.ReadingListTitleTextAppearance);
        headerView.setOverflowViewVisibility(VISIBLE);

        readingListId = getArguments().getLong(EXTRA_READING_LIST_ID);

        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        swipeRefreshLayout.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(() -> ReadingListsFragment.refreshSync(ReadingListFragment.this, swipeRefreshLayout));
        if (ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            swipeRefreshLayout.setEnabled(false);
        }

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
        updateReadingListData();
    }

    @Override public void onDestroyView() {
        disposables.clear();
        recyclerView.setAdapter(null);
        appBarLayout.removeOnOffsetChangedListener(appBarListener);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_reading_list, menu);
        if (showOverflowMenu) {
            inflater.inflate(R.menu.menu_reading_list_item, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem sortByNameItem = menu.findItem(R.id.menu_sort_by_name);
        MenuItem sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent);
        int sortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC);
        sortByNameItem.setTitle(sortMode == ReadingList.SORT_BY_NAME_ASC ? R.string.reading_list_sort_by_name_desc : R.string.reading_list_sort_by_name);
        sortByRecentItem.setTitle(sortMode == ReadingList.SORT_BY_RECENT_DESC ? R.string.reading_list_sort_by_recent_desc : R.string.reading_list_sort_by_recent);

        MenuItem searchItem = menu.findItem(R.id.menu_search_lists);
        MenuItem sortOptionsItem = menu.findItem(R.id.menu_sort_options);
        searchItem.getIcon().setColorFilter(toolbarExpanded ? getResources().getColor(android.R.color.white)
                : ResourceUtil.getThemedColor(requireContext(), R.attr.main_toolbar_icon_color), PorterDuff.Mode.SRC_IN);
        sortOptionsItem.getIcon().setColorFilter(toolbarExpanded ? getResources().getColor(android.R.color.white)
                : ResourceUtil.getThemedColor(requireContext(), R.attr.main_toolbar_icon_color), PorterDuff.Mode.SRC_IN);

        if (readingList != null && readingList.isDefault()) {
            if (menu.findItem(R.id.menu_reading_list_rename) != null) {
                menu.findItem(R.id.menu_reading_list_rename).setVisible(false);
            }
            if (menu.findItem(R.id.menu_reading_list_delete) != null) {
                menu.findItem(R.id.menu_reading_list_delete).setVisible(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_lists:
                getAppCompatActivity().startSupportActionMode(searchActionModeCallback);
                return true;
            case R.id.menu_sort_by_name:
                setSortMode(ReadingList.SORT_BY_NAME_ASC, ReadingList.SORT_BY_NAME_DESC);
                return true;
            case R.id.menu_sort_by_recent:
                setSortMode(ReadingList.SORT_BY_RECENT_DESC, ReadingList.SORT_BY_RECENT_ASC);
                return true;
            case R.id.menu_reading_list_rename:
                rename();
                return true;
            case R.id.menu_reading_list_delete:
                delete();
                return true;
            case R.id.menu_reading_list_save_all_offline:
                if (readingList != null) {
                    ReadingListBehaviorsUtil.INSTANCE.savePagesForOffline(requireActivity(), readingList.pages(), () -> {
                        adapter.notifyDataSetChanged();
                        update();
                    });
                }
                return true;
            case R.id.menu_reading_list_remove_all_offline:
                if (readingList != null) {
                    ReadingListBehaviorsUtil.INSTANCE.removePagesFromOffline(requireActivity(), readingList.pages(), () -> {
                        adapter.notifyDataSetChanged();
                        update();
                    });
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Nullable private Bundle getTransitionAnimationBundle(@NonNull PageTitle pageTitle) {
        // TODO: add future transition animations.
        return null;
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void update() {
        update(readingList);
    }

    private void update(@Nullable ReadingList readingList) {
        if (readingList == null) {
            return;
        }
        emptyView.setVisibility(readingList.pages().isEmpty() ? VISIBLE : GONE);
        headerView.setReadingList(readingList, ReadingListItemView.Description.DETAIL);
        headerImageView.setReadingList(readingList);
        ReadingList.sort(readingList, Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC));
        setSearchQuery();
        if (!toolbarExpanded) {
            toolBarLayout.setTitle(readingList.title());
        }
        if (!articleLimitMessageShown && readingList.pages().size() >= SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, readingList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            articleLimitMessageShown = true;
        }
    }

    private void updateReadingListData() {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().getFullListById(readingListId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    swipeRefreshLayout.setRefreshing(false);
                    readingList = list;
                    if (readingList != null) {
                        searchEmptyView.setEmptyText(getString(R.string.search_reading_list_no_results,
                                readingList.title()));
                    }
                    update();
                }, t -> {
                    // If we failed to retrieve the requested list, it means that the list is no
                    // longer in the database (likely removed due to sync).
                    // In this case, there's nothing for us to do, so just bail from the activity.
                    requireActivity().finish();
                }));
    }

    private void setSearchQuery() {
        setSearchQuery(currentSearchQuery);
    }

    private void setSearchQuery(@Nullable String query) {
        if (readingList == null) {
            return;
        }
        currentSearchQuery = query;
        if (TextUtils.isEmpty(query)) {
            displayedLists.clear();
            displayedLists.addAll(readingList.pages());
            adapter.notifyDataSetChanged();
            updateEmptyState(query);
        } else {
            ReadingListBehaviorsUtil.INSTANCE.searchListsAndPages(query, lists -> {
                displayedLists = lists;
                adapter.notifyDataSetChanged();
                updateEmptyState(query);
            });
        }
        touchCallback.setSwipeableEnabled(TextUtils.isEmpty(query));
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(GONE);
            recyclerView.setVisibility(VISIBLE);
            emptyView.setVisibility(displayedLists.isEmpty() ? VISIBLE : GONE);
        } else {
            recyclerView.setVisibility(displayedLists.isEmpty() ? GONE : VISIBLE);
            searchEmptyView.setVisibility(displayedLists.isEmpty() ? VISIBLE : GONE);
            emptyView.setVisibility(GONE);
        }
    }

    private void setSortMode(int sortModeAsc, int sortModeDesc) {
        int sortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC);
        if (sortMode != sortModeAsc) {
            sortMode = sortModeAsc;
        } else {
            sortMode = sortModeDesc;
        }
        Prefs.setReadingListPageSortMode(sortMode);
        requireActivity().invalidateOptionsMenu();
        update();
    }

    private void rename() {
        ReadingListBehaviorsUtil.INSTANCE.renameReadingList(requireActivity(), readingList, () -> {
            update();
            funnel.logModifyList(readingList, 0);
        });
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
        page.selected(!page.selected());
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
        for (Object list : displayedLists) {
            if (list instanceof ReadingListPage && ((ReadingListPage) list).selected()) {
                selectedCount++;
            }
        }
        return selectedCount;
    }

    private void unselectAllPages() {
        if (readingList == null) {
            return;
        }
        for (ReadingListPage page : readingList.pages()) {
            page.selected(false);
        }
        adapter.notifyDataSetChanged();
    }

    @NonNull
    private List<ReadingListPage> getSelectedPages() {
        List<ReadingListPage> result = new ArrayList<>();
        if (readingList == null) {
            return result;
        }
        for (Object list : displayedLists) {
            if (list instanceof ReadingListPage && ((ReadingListPage) list).selected()) {
                result.add((ReadingListPage) list);
                ((ReadingListPage) list).selected(false);
            }
        }
        return result;
    }

    private void deleteSelectedPages() {
        List<ReadingListPage> selectedPages = getSelectedPages();
        if (!selectedPages.isEmpty()) {

            ReadingListDbHelper.instance().markPagesForDeletion(readingList, selectedPages);
            readingList.pages().removeAll(selectedPages);

            funnel.logDeleteItem(readingList, 0);
            ReadingListBehaviorsUtil.INSTANCE.showDeletePagesUndoSnackbar(requireActivity(), readingList, selectedPages, this::updateReadingListData);
            update();
        }
    }

    private void addSelectedPagesToList() {
        List<ReadingListPage> selectedPages = getSelectedPages();
        if (!selectedPages.isEmpty()) {
            List<PageTitle> titles = new ArrayList<>();
            for (ReadingListPage page : selectedPages) {
                titles.add(ReadingListPage.toPageTitle(page));
            }
            bottomSheetPresenter.show(getChildFragmentManager(),
                    AddToReadingListDialog.newInstance(titles, READING_LIST_ACTIVITY));
            update();
        }
    }

    private void delete() {
        ReadingListBehaviorsUtil.INSTANCE.deleteReadingList(requireActivity(), readingList, true, () -> {
            startActivity(MainActivity.newIntent(requireActivity())
                    .putExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST, readingList.title()));
            requireActivity().finish();
        });
    }

    @Override
    public void onToggleItemOffline(@NonNull ReadingListPage page) {
        ReadingListBehaviorsUtil.INSTANCE.togglePageOffline(requireActivity(), page, () -> {
            adapter.notifyDataSetChanged();
            update();
        });
    }

    @Override
    public void onShareItem(@NonNull ReadingListPage page) {
        ShareUtil.shareText(getContext(), ReadingListPage.toPageTitle(page));
    }

    @Override
    public void onAddItemToOther(@NonNull ReadingListPage page) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page),
                        READING_LIST_ACTIVITY));
    }

    @Override
    public void onSelectItem(@NonNull ReadingListPage page) {
        if (actionMode == null || MultiSelectCallback.is(actionMode)) {
            beginMultiSelect();
            toggleSelectPage(page);
        }
    }

    @Override
    public void onDeleteItem(@NonNull ReadingListPage page) {
        List<ReadingList> listsContainPage = TextUtils.isEmpty(currentSearchQuery) ? Collections.singletonList(readingList) : ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page);
        ReadingListBehaviorsUtil.INSTANCE.deletePages(requireActivity(), listsContainPage, page, this::updateReadingListData, () -> {
            // TODO: need to verify the log of delete item since this action will delete multiple items in the same time.
            funnel.logDeleteItem(readingList, 0);
            update();
        });
    }

    private class AppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            if (verticalOffset > -appBarLayout.getTotalScrollRange() && showOverflowMenu) {
                showOverflowMenu = false;
                toolBarLayout.setTitle("");
                getAppCompatActivity().supportInvalidateOptionsMenu();
                toolbarExpanded = true;
            } else if (verticalOffset <= -appBarLayout.getTotalScrollRange() && !showOverflowMenu) {
                showOverflowMenu = true;
                toolBarLayout.setTitle(readingList != null ? readingList.title() : null);
                getAppCompatActivity().supportInvalidateOptionsMenu();
                toolbarExpanded = false;
            }

            DeviceUtil.updateStatusBarTheme(requireActivity(), toolbar,
                    actionMode == null && (appBarLayout.getTotalScrollRange() + verticalOffset) > appBarLayout.getTotalScrollRange() / 2);

            // prevent swiping when collapsing the view
            swipeRefreshLayout.setEnabled(verticalOffset == 0);
        }
    }

    private class ReadingListItemHolder extends DefaultViewHolder<View> {
        private ReadingListItemView itemView;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        void bindItem(ReadingList readingList) {
            itemView.setReadingList(readingList, ReadingListItemView.Description.SUMMARY);
            itemView.setSearchQuery(currentSearchQuery);
        }

        public ReadingListItemView getView() {
            return itemView;
        }
    }

    private class ReadingListPageItemHolder extends DefaultViewHolder<PageItemView<ReadingListPage>>
                implements SwipeableItemTouchHelperCallback.Callback {
        private ReadingListPage page;

        ReadingListPageItemHolder(PageItemView<ReadingListPage> itemView) {
            super(itemView);
        }

        void bindItem(ReadingListPage page) {
            this.page = page;
            getView().setItem(page);
            getView().setTitle(page.title());
            getView().setDescription(StringUtils.capitalize(page.description()));
            getView().setImageUrl(page.thumbUrl());
            getView().setSelected(page.selected());
            getView().setSecondaryActionIcon(page.saving() ? R.drawable.ic_download_in_progress : R.drawable.ic_download_circle_gray_24dp,
                    !page.offline() || page.saving());
            getView().setCircularProgressVisibility(page.downloadProgress() > 0 && page.downloadProgress() < MAX_PROGRESS);
            getView().setProgress(page.downloadProgress() == MAX_PROGRESS ? 0 : page.downloadProgress());
            getView().setSecondaryActionHint(R.string.reading_list_article_make_offline);
            getView().setSearchQuery(currentSearchQuery);
            getView().setListItemImageDimensions(getImageDimension(), getImageDimension());
            PageAvailableOfflineHandler.INSTANCE.check(page, available -> getView().setViewsGreyedOut(!available));

            if (!TextUtils.isEmpty(currentSearchQuery)) {
                getView().setTitleMaxLines(2);
                getView().setTitleEllipsis();
                getView().setDescriptionMaxLines(2);
                getView().setDescriptionEllipsis();
                getView().setUpChipGroup(ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page));
            } else {
                getView().hideChipGroup();
            }

        }

        @Override
        public void onSwipe() {
            if (TextUtils.isEmpty(currentSearchQuery)) {
                ReadingListBehaviorsUtil.INSTANCE.deletePages(requireActivity(), Collections.singletonList(readingList), page, ReadingListFragment.this::updateReadingListData, () -> {
                    funnel.logDeleteItem(readingList, 0);
                    update();
                });
            }
        }

        private int getImageDimension() {
            return DimenUtil.roundedDpToPx(TextUtils.isEmpty(currentSearchQuery)
                    ? DimenUtil.getDimension(R.dimen.view_list_card_item_image) : ARTICLE_ITEM_IMAGE_DIMENSION);
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
        private static final int TYPE_PAGE_ITEM = 2;

        private int getHeaderCount() {
            return (TextUtils.isEmpty(currentSearchQuery)) ? 1 : 0;
        }

        @Override
        public int getItemViewType(int position) {
            if (getHeaderCount() == 1 && position == 0) {
                return TYPE_HEADER;
            } else if (displayedLists.get(position - getHeaderCount()) instanceof ReadingList) {
                return TYPE_ITEM;
            } else {
                return TYPE_PAGE_ITEM;
            }
        }

        @Override
        public int getItemCount() {
            return getHeaderCount() + displayedLists.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            if (type == TYPE_ITEM) {
                ReadingListItemView view = new ReadingListItemView(getContext());
                return new ReadingListItemHolder(view);
            } else if (type == TYPE_HEADER) {
                return new ReadingListHeaderHolder(headerView);
            } else {
                return new ReadingListPageItemHolder(new PageItemView<>(requireContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (readingList != null) {
                if (holder instanceof ReadingListItemHolder) {
                    ((ReadingListItemHolder) holder).bindItem((ReadingList) displayedLists.get(pos - getHeaderCount()));
                } else if (holder instanceof  ReadingListPageItemHolder) {
                    ((ReadingListPageItemHolder) holder).bindItem((ReadingListPage) displayedLists.get(pos - getHeaderCount()));
                }
            }
        }

        @Override public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof ReadingListItemHolder) {
                ((ReadingListItemHolder) holder).getView().setCallback(readingListItemCallback);
            } else if (holder instanceof  ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).getView().setCallback(readingListPageItemCallback);
            }
        }

        @Override public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
            if (holder instanceof ReadingListItemHolder) {
                ((ReadingListItemHolder) holder).getView().setCallback(null);
            } else if (holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).getView().setCallback(null);
            }
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class HeaderCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
        }

        @Override
        public void onRename(@NonNull ReadingList readingList) {
            rename();
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
            delete();
        }

        @Override
        public void onSaveAllOffline(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.savePagesForOffline(requireActivity(), readingList.pages(), () -> {
                adapter.notifyDataSetChanged();
                update();
            });
        }

        @Override
        public void onRemoveAllOffline(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.removePagesFromOffline(requireActivity(), readingList.pages(), () -> {
                adapter.notifyDataSetChanged();
                update();
            });
        }
    }

    private class ReadingListItemCallback implements ReadingListItemView.Callback {

        @Override
        public void onClick(@NonNull ReadingList readingList) {
            if (actionMode != null) {
                actionMode.finish();
            }
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList));
        }

        @Override
        public void onRename(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.renameReadingList(requireActivity(), readingList, () -> update(readingList));
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.deleteReadingList(requireActivity(), readingList, true, () -> {
                ReadingListBehaviorsUtil.INSTANCE.showDeleteListUndoSnackbar(requireActivity(), readingList, ReadingListFragment.this::setSearchQuery);
                setSearchQuery();
            });
        }

        @Override
        public void onSaveAllOffline(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.savePagesForOffline(requireActivity(), readingList.pages(), ReadingListFragment.this::setSearchQuery);
        }

        @Override
        public void onRemoveAllOffline(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.removePagesFromOffline(requireActivity(), readingList.pages(), ReadingListFragment.this::setSearchQuery);
        }
    }

    private class ReadingListPageItemCallback implements PageItemView.Callback<ReadingListPage> {
        @Override
        public void onClick(@Nullable ReadingListPage page) {
            if (MultiSelectCallback.is(actionMode)) {
                toggleSelectPage(page);
            } else if (page != null) {
                PageTitle title = ReadingListPage.toPageTitle(page);
                HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);

                page.touch();
                Completable.fromAction(() -> {
                    ReadingListDbHelper.instance().updateLists(ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page), false);
                    ReadingListDbHelper.instance().updatePage(page);
                }).subscribeOn(Schedulers.io()).subscribe();

                startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
            }
        }

        @Override
        public boolean onLongClick(@Nullable ReadingListPage page) {
            if (page == null) {
                return false;
            }
            bottomSheetPresenter.show(getChildFragmentManager(),
                    ReadingListItemActionsDialog.newInstance(TextUtils.isEmpty(currentSearchQuery)
                            ? Collections.singletonList(readingList) : ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page), page, actionMode != null));
            return true;
        }

        @Override
        public void onThumbClick(@Nullable ReadingListPage item) {
            onClick(item);
        }

        @Override
        public void onActionClick(@Nullable ReadingListPage page, @NonNull View view) {
            if (page == null) {
                return;
            }
            bottomSheetPresenter.show(getChildFragmentManager(),
                    ReadingListItemActionsDialog.newInstance(TextUtils.isEmpty(currentSearchQuery)
                            ? Collections.singletonList(readingList) : ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page), page, actionMode != null));
        }

        @Override
        public void onSecondaryActionClick(@Nullable ReadingListPage page, @NonNull View view) {
            if (page != null) {
                if (Prefs.isDownloadOnlyOverWiFiEnabled() && !DeviceUtil.isOnWiFi()
                        && page.status() == ReadingListPage.STATUS_QUEUE_FOR_SAVE) {
                    page.offline(false);
                }

                if (page.saving()) {
                    Toast.makeText(getContext(), R.string.reading_list_article_save_in_progress, Toast.LENGTH_LONG).show();
                } else {
                    ReadingListBehaviorsUtil.INSTANCE.toggleOffline(requireActivity(), page, () -> {
                        adapter.notifyDataSetChanged();
                        update();
                    });
                }
            }
        }

        @Override
        public void onListChipClick(@NonNull ReadingList readingList) {
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList));
        }
    }

    private void setStatusBarActionMode(boolean inActionMode) {
        DeviceUtil.updateStatusBarTheme(requireActivity(), toolbar, toolbarExpanded && !inActionMode);
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        requireActivity().getWindow().setStatusBarColor(!inActionMode
                ? Color.TRANSPARENT : ResourceUtil.getThemedColor(requireActivity(), R.attr.main_status_bar_color));
    }

    private class SearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            recyclerView.stopScroll();
            appBarLayout.setExpanded(false, false);
            setStatusBarActionMode(true);
            ViewUtil.finishActionModeWhenTappingOnView(getView(), actionMode);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            setSearchQuery(s.trim());
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            actionMode = null;
            currentSearchQuery = null;
            setStatusBarActionMode(false);
            updateReadingListData();
        }

        @Override
        protected String getSearchHintString() {
            return getString(R.string.search_hint_search_my_lists_and_articles);
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
        @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            super.onCreateActionMode(mode, menu);
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_reading_list, menu);
            actionMode = mode;
            setStatusBarActionMode(true);
            return true;
        }

        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_delete_selected:
                    onDeleteSelected();
                    finishActionMode();
                    return true;
                case R.id.menu_remove_from_offline:
                    ReadingListBehaviorsUtil.INSTANCE.removePagesFromOffline(requireActivity(), getSelectedPages(), () -> {
                        adapter.notifyDataSetChanged();
                        update();
                    });
                    finishActionMode();
                    return true;
                case R.id.menu_save_for_offline:
                    ReadingListBehaviorsUtil.INSTANCE.savePagesForOffline(requireActivity(), getSelectedPages(), () -> {
                        adapter.notifyDataSetChanged();
                        update();
                    });
                    finishActionMode();
                    return true;
                case R.id.menu_add_to_another_list:
                    addSelectedPagesToList();
                    finishActionMode();
                    return true;
                default:
            }
            return false;
        }

        @Override protected void onDeleteSelected() {
            deleteSelectedPages();
        }

        @Override public void onDestroyActionMode(ActionMode mode) {
            unselectAllPages();
            actionMode = null;
            setStatusBarActionMode(false);
            super.onDestroyActionMode(mode);
        }
    }

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ReadingListSyncEvent) {
                updateReadingListData();
            } else if (event instanceof PageDownloadEvent) {
                int pagePosition = getPagePositionInList(((PageDownloadEvent) event).getPage());
                if (pagePosition != -1 && displayedLists.get(pagePosition) instanceof ReadingListPage) {
                    ((ReadingListPage) displayedLists.get(pagePosition)).downloadProgress(((PageDownloadEvent) event).getPage().downloadProgress());
                    adapter.notifyItemChanged(pagePosition + 1);
                }
            }
        }
    }

    private int getPagePositionInList(ReadingListPage page) {
        for (Object list : displayedLists) {
            if (list instanceof ReadingListPage && ((ReadingListPage) list).id() == page.id()) {
                return displayedLists.indexOf(list);
            }
        }
        return -1;
    }
}
