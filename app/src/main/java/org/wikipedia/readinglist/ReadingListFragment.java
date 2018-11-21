package org.wikipedia.readinglist;

import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.PluralsRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
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

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.events.PageDownloadEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.main.MainActivity;
import org.wikipedia.main.floatingqueue.FloatingQueueView;
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
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MarginItemDecoration;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
import static org.wikipedia.readinglist.ReadingListActivity.EXTRA_READING_LIST_ID;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.views.CircularProgressBar.MAX_PROGRESS;
import static org.wikipedia.views.CircularProgressBar.MIN_PROGRESS;

public class ReadingListFragment extends Fragment implements
        ReadingListItemActionsDialog.Callback, FloatingQueueView.Callback{
    @BindView(R.id.reading_list_toolbar) Toolbar toolbar;
    @BindView(R.id.reading_list_toolbar_container) CollapsingToolbarLayout toolBarLayout;
    @BindView(R.id.reading_list_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.reading_list_header) ReadingListHeaderView headerImageView;
    @BindView(R.id.reading_list_contents) RecyclerView recyclerView;
    @BindView(R.id.reading_list_empty_text) TextView emptyView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.reading_list_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.floating_queue_view) FloatingQueueView floatingQueueView;
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

        ItemTouchHelper.Callback touchCallback = new SwipeableItemTouchHelperCallback(requireContext());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.addItemDecoration(new DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, false));
        recyclerView.addItemDecoration(new MarginItemDecoration(0, 0, 0, DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.floating_queue_container_height))) {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                if (parent.getChildAdapterPosition(view) == adapter.getItemCount() - 1 && floatingQueueView.getVisibility() == View.VISIBLE) {
                    super.getItemOffsets(outRect, view, parent, state);
                }
            }
        });

        headerView = new ReadingListItemView(getContext());
        headerView.setCallback(headerCallback);
        headerView.setClickable(false);
        headerView.setThumbnailVisible(false);
        headerView.setTitleTextAppearance(R.style.ReadingListTitleTextAppearance);

        readingListId = getArguments().getLong(EXTRA_READING_LIST_ID);

        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        floatingQueueView.setCallback(this);
        swipeRefreshLayout.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(() -> ReadingListsFragment.refreshSync(ReadingListFragment.this, swipeRefreshLayout));
        if (ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            swipeRefreshLayout.setEnabled(false);
        }
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> swipeRefreshLayout.setEnabled(verticalOffset == 0));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        floatingQueueView.animation(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateReadingListData();
        floatingQueueView.update();
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


    @Override
    public void onFloatingQueueClicked(@NonNull PageTitle title) {
        startActivity(PageActivity.newIntentForExistingTab(requireContext(),
                new HistoryEntry(title, HistoryEntry.SOURCE_FLOATING_QUEUE), title), getTransitionAnimationBundle(title));
    }

    private Bundle getTransitionAnimationBundle(@NonNull PageTitle pageTitle) {
        return pageTitle.getThumbUrl() != null ? ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
                floatingQueueView.getImageView(), ViewCompat.getTransitionName(floatingQueueView.getImageView())).toBundle() : null;
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void update() {
        if (readingList == null) {
            return;
        }
        emptyView.setVisibility(readingList.pages().isEmpty() ? VISIBLE : GONE);
        headerView.setReadingList(readingList, ReadingListItemView.Description.DETAIL);
        headerImageView.setReadingList(readingList);
        ReadingList.sort(readingList, Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC));
        setSearchQuery(currentSearchQuery);
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
            searchEmptyView.setVisibility(GONE);
            recyclerView.setVisibility(VISIBLE);
            emptyView.setVisibility(displayedPages.isEmpty() ? VISIBLE : GONE);
        } else {
            recyclerView.setVisibility(displayedPages.isEmpty() ? GONE : VISIBLE);
            searchEmptyView.setVisibility(displayedPages.isEmpty() ? VISIBLE : GONE);
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

        ReadingListTitleDialog.readingListTitleDialog(requireContext(), readingList.title(), readingList.description(), existingTitles,
                (text, description) -> {
                    readingList.title(text);
                    readingList.description(description);
                    readingList.dirty(true);
                    ReadingListDbHelper.instance().updateList(readingList, true);
                    update();
                    funnel.logModifyList(readingList, 0);
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
            ReadingListDbHelper.instance().markPagesForOffline(selectedPages, false, false);
            showMultiSelectOfflineStateChangeSnackbar(selectedPages, false);
            adapter.notifyDataSetChanged();
            update();
        }
    }

    private void saveSelectedPagesForOffline(List<ReadingListPage> selectedPages) {
        if (shouldForceDownloadOverMobileData()) {
            ReadingListsFragment.showMobileDataWarningDialog(requireActivity(), (dialog, which)
                    -> saveSelectedPagesForOffline(selectedPages, true));
        } else {
            saveSelectedPagesForOffline(selectedPages, !Prefs.isDownloadingReadingListArticlesEnabled());
        }
    }

    private void saveSelectedPagesForOffline(@NonNull List<ReadingListPage> selectedPages, boolean forcedSave) {
        if (!selectedPages.isEmpty()) {
            for (ReadingListPage page : selectedPages) {
                resetPageProgress(page);
            }
            ReadingListDbHelper.instance().markPagesForOffline(selectedPages, true, forcedSave);
            showMultiSelectOfflineStateChangeSnackbar(selectedPages, true);
            adapter.notifyDataSetChanged();
            update();
        }
    }

    private void resetPageProgress(ReadingListPage page) {
        if (!page.offline()) {
            page.downloadProgress(MIN_PROGRESS);
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
        AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity());
        alert.setMessage(getString(R.string.reading_list_delete_confirm, readingList.title()));
        alert.setPositiveButton(android.R.string.yes, (dialog, id) -> {
            startActivity(MainActivity.newIntent(requireActivity())
                    .putExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST, readingList.title()));
            requireActivity().finish();
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
            disposables.add(Observable.fromCallable(() -> {
                List<ReadingListPage> occurrences = ReadingListDbHelper.instance().getAllPageOccurrences(ReadingListPage.toPageTitle(page));
                return ReadingListDbHelper.instance().getListsFromPageOccurrences(occurrences);
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(lists -> {
                        if (lists.size() > 1) {
                            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                    .setTitle(R.string.reading_list_confirm_remove_article_from_offline_title)
                                    .setMessage(getConfirmToggleOfflineMessage(page, lists))
                                    .setPositiveButton(R.string.reading_list_confirm_remove_article_from_offline, (dialog1, which) -> toggleOffline(page))
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .create();
                            dialog.show();
                        } else {
                            toggleOffline(page);
                        }
                    }, L::w));
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
        resetPageProgress(page);
        if (shouldForceDownloadOverMobileData()) {
            ReadingListsFragment.showMobileDataWarningDialog(requireActivity(), (dialog, which)
                    -> toggleOffline(page, true));
        }  else {
            toggleOffline(page, !Prefs.isDownloadingReadingListArticlesEnabled());
        }
    }

    private void toggleOffline(@NonNull ReadingListPage page, boolean forcedSave) {
        ReadingListDbHelper.instance().markPageForOffline(page, !page.offline(), forcedSave);
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

    private boolean shouldForceDownloadOverMobileData() {
        return Prefs.isDownloadOnlyOverWiFiEnabled() && !DeviceUtil.isOnWiFi();
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

            recyclerView.post(() -> {
                if (isAdded()) {
                    DeviceUtil.updateStatusBarTheme(requireActivity(), toolbar, toolbarExpanded);
                }
            });
            // prevent swiping when collapsing the view
            swipeRefreshLayout.setEnabled(verticalOffset == 0);
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
            getView().setDescription(StringUtils.capitalize(page.description()));
            getView().setImageUrl(page.thumbUrl());
            getView().setSelected(page.selected());
            getView().setActionIcon(R.drawable.ic_more_vert_white_24dp);
            getView().setActionTint(R.attr.material_theme_de_emphasised_color);
            getView().setActionHint(R.string.abc_action_menu_overflow_description);
            getView().setSecondaryActionIcon(page.saving() ? R.drawable.ic_download_in_progress : R.drawable.ic_download_circle_gray_24dp,
                    !page.offline() || page.saving());
            getView().setCircularProgressVisibility(page.downloadProgress() > 0 && page.downloadProgress() < MAX_PROGRESS);
            getView().setProgress(page.downloadProgress() == MAX_PROGRESS ? 0 : page.downloadProgress());
            getView().setSecondaryActionHint(R.string.reading_list_article_make_offline);
            PageAvailableOfflineHandler.INSTANCE.check(page, available -> getView().setViewsGreyedOut(!available));
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
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            if (type == TYPE_HEADER) {
                return new ReadingListHeaderHolder(headerView);
            }
            return new ReadingListPageItemHolder(new PageItemView<>(requireContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (readingList != null && holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).bindItem(displayedPages.get(pos - 1));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).getView().setCallback(itemCallback);
            }
        }

        @Override public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
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
                Completable.fromAction(() -> {
                    ReadingListDbHelper.instance().updateList(readingList, false);
                    ReadingListDbHelper.instance().updatePage(page);
                }).subscribeOn(Schedulers.io()).subscribe();

                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()), getTransitionAnimationBundle(entry.getTitle()));
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
                if (shouldForceDownloadOverMobileData()
                        && page.status() == ReadingListPage.STATUS_QUEUE_FOR_SAVE) {
                    page.offline(false);
                }

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
            floatingQueueView.hide();
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
            floatingQueueView.show();
            setSearchQuery(null);
        }

        @Override
        protected String getSearchHintString() {
            return getString(R.string.search_hint_search_reading_list);
        }

        @Override
        protected boolean finishActionModeIfKeyboardHiding() {
            return true;
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

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) throws Exception {
            if (event instanceof ReadingListSyncEvent) {
                updateReadingListData();
            } else if (event instanceof PageDownloadEvent) {
                int pagePosition = getPagePositionInList(((PageDownloadEvent) event).getPage());
                if (pagePosition != -1) {
                    displayedPages.get(pagePosition).downloadProgress(((PageDownloadEvent) event).getPage().downloadProgress());
                    adapter.notifyItemChanged(pagePosition + 1);
                }
            }
        }
    }

    private int getPagePositionInList(ReadingListPage page) {
        for (ReadingListPage readingListPage : displayedPages) {
            if (readingListPage.id() == page.id()) {
                return displayedPages.indexOf(readingListPage);
            }
        }
        return -1;
    }
}
