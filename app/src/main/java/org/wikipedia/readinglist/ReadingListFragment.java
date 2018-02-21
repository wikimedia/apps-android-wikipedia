package org.wikipedia.readinglist;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.main.MainActivity;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.readinglist.sync.ReadingListSyncEvent;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.TextInputDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.readinglist.ReadingListActivity.EXTRA_READING_LIST_ID;

public class ReadingListFragment extends Fragment implements ReadingListItemActionsDialog.Callback {
    @BindView(R.id.reading_list_toolbar) Toolbar toolbar;
    @BindView(R.id.reading_list_toolbar_container) CollapsingToolbarLayout toolBarLayout;
    @BindView(R.id.reading_list_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.reading_list_header) ReadingListHeaderView headerImageView;
    @BindView(R.id.reading_list_contents) RecyclerView recyclerView;
    @BindView(R.id.reading_list_empty_text) TextView emptyView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    private Unbinder unbinder;

    @NonNull private final EventBusMethods eventBusMethods = new EventBusMethods();

    @Nullable private ReadingList readingList;
    private long readingListId;

    private ReadingListPageItemAdapter adapter = new ReadingListPageItemAdapter();
    private ReadingListItemView headerView;
    @Nullable private ActionMode actionMode;
    private AppBarListener appBarListener = new AppBarListener();
    private boolean showOverflowMenu;

    private ReadingListsFunnel funnel = new ReadingListsFunnel();
    private HeaderCallback headerCallback = new HeaderCallback();
    private ItemCallback itemCallback = new ItemCallback();
    private SearchCallback searchActionModeCallback = new SearchCallback();
    private MultiSelectActionModeCallback multiSelectActionModeCallback = new MultiSelectCallback();
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private boolean toolbarExpanded = true;

    @NonNull private List<ReadingListPage> displayedPages = new ArrayList<>();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_reading_list, container, false);
        unbinder = ButterKnife.bind(this, view);

        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        appBarLayout.addOnOffsetChangedListener(appBarListener);
        toolBarLayout.setCollapsedTitleTextColor(Color.WHITE);

        ItemTouchHelper.Callback touchCallback = new SwipeableItemTouchHelperCallback(getContext());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(), R.attr.list_separator_drawable));

        headerView = new ReadingListItemView(getContext());
        headerView.setCallback(headerCallback);
        headerView.setClickable(false);
        headerView.setThumbnailVisible(false);
        headerView.setShowDescriptionEmptyHint(true);
        headerView.setTitleTextAppearance(R.style.ReadingListTitleTextAppearance);

        readingListId = getArguments().getLong(EXTRA_READING_LIST_ID);

        WikipediaApp.getInstance().getBus().register(eventBusMethods);

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
        WikipediaApp.getInstance().getBus().unregister(eventBusMethods);
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
        int sortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC);
        sortByNameItem.setTitle(sortMode == ReadingList.SORT_BY_NAME_ASC ? R.string.reading_list_sort_by_name_desc : R.string.reading_list_sort_by_name);
        sortByRecentItem.setTitle(sortMode == ReadingList.SORT_BY_RECENT_DESC ? R.string.reading_list_sort_by_recent_desc : R.string.reading_list_sort_by_recent);
        if (readingList != null && readingList.isDefault()) {
            if (menu.findItem(R.id.menu_reading_list_rename) != null) {
                menu.findItem(R.id.menu_reading_list_rename).setVisible(false);
            }
            if (menu.findItem(R.id.menu_reading_list_edit_description) != null) {
                menu.findItem(R.id.menu_reading_list_edit_description).setVisible(false);
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
            case R.id.menu_reading_list_edit_description:
                editDescription();
                return true;
            case R.id.menu_reading_list_delete:
                delete();
                return true;
            case R.id.menu_reading_list_save_all_offline:
                if (readingList != null) {
                    saveSelectedPagesForOffline(readingList.pages());
                }
                return true;
            case R.id.menu_reading_list_remove_all_offline:
                if (readingList != null) {
                    removeSelectedPagesFromOffline(readingList.pages());
                }
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
        emptyView.setVisibility(readingList.pages().isEmpty() ? View.VISIBLE : View.GONE);
        headerView.setReadingList(readingList, ReadingListItemView.Description.DETAIL);
        headerImageView.setReadingList(readingList);
        ReadingList.sort(readingList, Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC));
        setSearchQuery(currentSearchQuery);
        if (!toolbarExpanded) {
            toolBarLayout.setTitle(readingList.title());
        }
        if (!articleLimitMessageShown && readingList.pages().size() >= Constants.MAX_READING_LIST_ARTICLE_LIMIT) {
            String message = getString(R.string.reading_list_article_limit_message, readingList.title(), Constants.MAX_READING_LIST_ARTICLE_LIMIT);
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            articleLimitMessageShown = true;
        }
    }

    private void updateReadingListData() {
        CallbackTask.execute(() -> ReadingListDbHelper.instance().getFullListById(readingListId), new CallbackTask.DefaultCallback<ReadingList>() {
            @Override
            public void success(ReadingList list) {
                if (getActivity() == null) {
                    return;
                }
                readingList = list;
                if (readingList != null) {
                    searchEmptyView.setEmptyText(getString(R.string.search_reading_list_no_results,
                            readingList.title()));
                }
                update();
            }
        });
    }

    private void setSearchQuery(@Nullable String query) {
        if (readingList == null) {
            return;
        }
        currentSearchQuery = query;
        displayedPages.clear();
        if (TextUtils.isEmpty(query)) {
            displayedPages.addAll(readingList.pages());
        } else {
            query = query.toUpperCase(Locale.getDefault());
            for (ReadingListPage page : readingList.pages()) {
                if (page.title().toUpperCase(Locale.getDefault())
                        .contains(query.toUpperCase(Locale.getDefault()))) {
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
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(displayedPages.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            recyclerView.setVisibility(displayedPages.isEmpty() ? View.GONE : View.VISIBLE);
            searchEmptyView.setVisibility(displayedPages.isEmpty() ? View.VISIBLE : View.GONE);
            emptyView.setVisibility(View.GONE);
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
        getActivity().invalidateOptionsMenu();
        update();
    }

    private void showMultiSelectOfflineStateChangeSnackbar(List<ReadingListPage> pages, boolean offline) {
        String message = offline
                ? getQuantityString(R.plurals.reading_list_article_offline_message, pages.size())
                : getQuantityString(R.plurals.reading_list_article_not_offline_message, pages.size());
        FeedbackUtil.showMessage(getActivity(), message);
    }

    private void showDeleteItemsUndoSnackbar(final ReadingList readingList, final List<ReadingListPage> pages) {
        String message = pages.size() == 1
                ? String.format(getString(R.string.reading_list_item_deleted), pages.get(0).title())
                : String.format(getString(R.string.reading_list_items_deleted), pages.size());
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(), message,
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, v -> {
            List<ReadingListPage> newPages = new ArrayList<>();
            for (ReadingListPage page : pages) {
                newPages.add(new ReadingListPage(ReadingListPage.toPageTitle(page)));
            }
            ReadingListDbHelper.instance().addPagesToList(readingList, newPages, true);
            readingList.pages().addAll(newPages);
            updateReadingListData();
        });
        snackbar.show();
    }

    private void rename() {
        if (readingList == null) {
            return;
        } else if (readingList.isDefault()) {
            L.w("Attempted to rename default list.");
            return;
        }

        List<ReadingList> tempLists = ReadingListDbHelper.instance().getAllListsWithoutContents();
        List<String> existingTitles = new ArrayList<>();
        for (ReadingList list : tempLists) {
            existingTitles.add(list.title());
        }
        existingTitles.remove(readingList.title());

        ReadingListTitleDialog.readingListTitleDialog(getContext(), readingList.title(), existingTitles,
                text -> {
                    readingList.title(text.toString());
                    ReadingListDbHelper.instance().updateList(readingList, true);
                    update();
                    funnel.logModifyList(readingList, 0);
                }).show();
    }

    private void editDescription() {
        if (readingList == null) {
            return;
        } else if (readingList.isDefault()) {
            L.w("Attempted to edit description of default list.");
            return;
        }
        TextInputDialog.newInstance(getContext(), new TextInputDialog.DefaultCallback() {
            @Override
            public void onShow(@NonNull TextInputDialog dialog) {
                dialog.setHint(R.string.reading_list_description_hint);
                dialog.setText(readingList.description());
            }

            @Override
            public void onSuccess(@NonNull CharSequence text) {

                readingList.description(text.toString());
                readingList.dirty(true);
                ReadingListDbHelper.instance().updateList(readingList, true);

                update();
                funnel.logModifyList(readingList, 0);
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
        for (ReadingListPage page : displayedPages) {
            if (page.selected()) {
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
        for (ReadingListPage page : displayedPages) {
            if (page.selected()) {
                result.add(page);
                page.selected(false);
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
            showDeleteItemsUndoSnackbar(readingList, selectedPages);
            update();
        }
    }

    private void removeSelectedPagesFromOffline(List<ReadingListPage> selectedPages) {
        if (!selectedPages.isEmpty()) {
            ReadingListDbHelper.instance().markPagesForOffline(selectedPages, false);
            showMultiSelectOfflineStateChangeSnackbar(selectedPages, false);
            adapter.notifyDataSetChanged();
            update();
        }
    }

    private void saveSelectedPagesForOffline(List<ReadingListPage> selectedPages) {
        if (!selectedPages.isEmpty()) {
            ReadingListDbHelper.instance().markPagesForOffline(selectedPages, true);
            showMultiSelectOfflineStateChangeSnackbar(selectedPages, true);
            adapter.notifyDataSetChanged();
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
                    AddToReadingListDialog.newInstance(titles,
                            AddToReadingListDialog.InvokeSource.READING_LIST_ACTIVITY));
            update();
        }
    }

    private void deleteSinglePage(@Nullable ReadingListPage page) {
        if (readingList == null || page == null) {
            return;
        }
        showDeleteItemsUndoSnackbar(readingList, Collections.singletonList(page));
        ReadingListDbHelper.instance().markPagesForDeletion(readingList, Collections.singletonList(page));
        readingList.pages().remove(page);
        funnel.logDeleteItem(readingList, 0);
        update();
    }

    private void delete() {
        if (readingList == null) {
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(getString(R.string.reading_list_delete_confirm, readingList.title()));
        alert.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            startActivity(MainActivity.newIntent(getActivity())
                    .putExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST, readingList.title()));
            getActivity().finish();
        });
        alert.setNegativeButton(android.R.string.no, null);
        alert.create().show();
    }

    @Override
    public void onToggleItemOffline(int pageIndex) {
        ReadingListPage page = readingList == null ? null : readingList.pages().get(pageIndex);
        if (page == null) {
            return;
        }
        if (page.offline()) {
            // If the page belongs to more than one list, then warn the user.
            CallbackTask.execute(() -> {
                List<ReadingListPage> occurrences = ReadingListDbHelper.instance().getAllPageOccurrences(ReadingListPage.toPageTitle(page));
                return ReadingListDbHelper.instance().getListsFromPageOccurrences(occurrences);
            }, new CallbackTask.DefaultCallback<List<ReadingList>>() {
                @SuppressWarnings("checkstyle:magicnumber")
                @Override public void success(List<ReadingList> lists) {
                    if (!isAdded()) {
                        return;
                    }
                    if (lists.size() > 1) {
                        AlertDialog dialog = new AlertDialog.Builder(getContext())
                                .setTitle(R.string.reading_list_confirm_remove_article_from_offline_title)
                                .setMessage(getConfirmToggleOfflineMessage(page, lists))
                                .setPositiveButton(R.string.reading_list_confirm_remove_article_from_offline, (dialog1, which) -> toggleOffline(page))
                                .setNegativeButton(android.R.string.cancel, null)
                                .create();
                        dialog.show();
                        TextView text = dialog.findViewById(android.R.id.message);
                        text.setLineSpacing(0, 1.3f);
                    } else {
                        toggleOffline(page);
                    }
                }
            });
        } else {
            toggleOffline(page);
        }
    }

    @Override
    public void onShareItem(int pageIndex) {
        ReadingListPage page = readingList == null ? null : readingList.pages().get(pageIndex);
        if (page != null) {
            ShareUtil.shareText(getContext(), ReadingListPage.toPageTitle(page));
        }
    }

    @Override
    public void onAddItemToOther(int pageIndex) {
        ReadingListPage page = readingList == null ? null : readingList.pages().get(pageIndex);
        if (page != null) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page),
                            AddToReadingListDialog.InvokeSource.READING_LIST_ACTIVITY));
        }
    }

    @Override
    public void onDeleteItem(int pageIndex) {
        ReadingListPage page = readingList == null ? null : readingList.pages().get(pageIndex);
        deleteSinglePage(page);
    }

    private void toggleOffline(@NonNull ReadingListPage page) {
        ReadingListDbHelper.instance().markPageForOffline(page, !page.offline());
        if (getActivity() != null) {
            FeedbackUtil.showMessage(getActivity(), page.offline()
                    ? getQuantityString(R.plurals.reading_list_article_offline_message, 1)
                    : getQuantityString(R.plurals.reading_list_article_not_offline_message, 1));
            adapter.notifyDataSetChanged();
            update();
        }
    }

    @NonNull private Spanned getConfirmToggleOfflineMessage(@NonNull ReadingListPage page, @NonNull List<ReadingList> lists) {
        String result = getString(R.string.reading_list_confirm_remove_article_from_offline_message,
                "<b>" +  page.title() + "</b>");
        for (ReadingList list : lists) {
            result += "<br>&nbsp;&nbsp;<b>&#8226; " + list.title() + "</b>";
        }
        return StringUtil.fromHtml(result);
    }

    @NonNull private String getQuantityString(@PluralsRes int id, int quantity, Object... formatArgs) {
        return getResources().getQuantityString(id, quantity, formatArgs);
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
        public void onEditDescription(@NonNull ReadingList readingList) {
            editDescription();
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
            delete();
        }

        @Override
        public void onSaveAllOffline(@NonNull ReadingList readingList) {
            saveSelectedPagesForOffline(readingList.pages());
        }

        @Override
        public void onRemoveAllOffline(@NonNull ReadingList readingList) {
            removeSelectedPagesFromOffline(readingList.pages());
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
            getView().setDescription(page.description());
            getView().setImageUrl(page.thumbUrl());
            getView().setSelected(page.selected());
            getView().setActionIcon(R.drawable.ic_more_vert_white_24dp);
            getView().setActionHint(R.string.abc_action_menu_overflow_description);
            getView().setSecondaryActionIcon(page.saving()
                    ? R.drawable.ic_download_started : R.drawable.ic_download_circle_gray_24dp,
                    !page.offline() || page.saving());
            getView().setSecondaryActionHint(R.string.reading_list_article_make_offline);
        }

        @Override
        public void onSwipe() {
            deleteSinglePage(page);
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
            return new ReadingListPageItemHolder(new PageItemView<>(getContext()));
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
                PageTitle title = ReadingListPage.toPageTitle(page);
                HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);

                page.touch();
                CallbackTask.execute(() -> {
                    ReadingListDbHelper.instance().updateList(readingList, false);
                    ReadingListDbHelper.instance().updatePage(page);
                });

                startActivity(PageActivity.newIntentForNewTab(getContext(), entry, entry.getTitle()));
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
        public void onActionClick(@Nullable ReadingListPage page, @NonNull View view) {
            if (page == null || readingList == null) {
                return;
            }
            bottomSheetPresenter.show(getChildFragmentManager(),
                    ReadingListItemActionsDialog.newInstance(page, readingList));
        }

        @Override
        public void onSecondaryActionClick(@Nullable ReadingListPage page, @NonNull View view) {
            if (page != null) {
                if (page.saving()) {
                    Toast.makeText(getContext(), R.string.reading_list_article_save_in_progress, Toast.LENGTH_LONG).show();
                } else {
                    toggleOffline(page);
                }
            }
        }
    }

    private class SearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            recyclerView.stopScroll();
            appBarLayout.setExpanded(false, false);
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
            setSearchQuery(null);
        }

        @Override
        protected String getSearchHintString() {
            return getString(R.string.search_hint_search_reading_list);
        }
    }

    private class MultiSelectCallback extends MultiSelectActionModeCallback {
        @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            super.onCreateActionMode(mode, menu);
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_reading_list, menu);
            actionMode = mode;
            return true;
        }

        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_delete_selected:
                    onDeleteSelected();
                    finishActionMode();
                    return true;
                case R.id.menu_remove_from_offline:
                    removeSelectedPagesFromOffline(getSelectedPages());
                    finishActionMode();
                    return true;
                case R.id.menu_save_for_offline:
                    saveSelectedPagesForOffline(getSelectedPages());
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
            super.onDestroyActionMode(mode);
        }
    }

    private class EventBusMethods {
        @Subscribe public void on(@NonNull ReadingListSyncEvent event) {
            if (isAdded()) {
                updateReadingListData();
            }
        }
    }
}
