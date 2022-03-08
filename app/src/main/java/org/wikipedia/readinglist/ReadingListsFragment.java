package org.wikipedia.readinglist;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
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
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.events.ArticleSavedOrDeletedEvent;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.main.MainActivity;
import org.wikipedia.main.MainFragment;
import org.wikipedia.onboarding.OnboardingView;
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
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MarginItemDecoration;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.ReadingListsOverflowView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.readinglist.database.ReadingList.SORT_BY_NAME_ASC;
import static org.wikipedia.readinglist.database.ReadingList.SORT_BY_NAME_DESC;
import static org.wikipedia.readinglist.database.ReadingList.SORT_BY_RECENT_ASC;
import static org.wikipedia.readinglist.database.ReadingList.SORT_BY_RECENT_DESC;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.views.CircularProgressBar.MAX_PROGRESS;

public class ReadingListsFragment extends Fragment implements
        SortReadingListsDialog.Callback, ReadingListItemActionsDialog.Callback {
    private Unbinder unbinder;
    @BindView(R.id.reading_list_content_container) ViewGroup contentContainer;
    @BindView(R.id.reading_list_list) RecyclerView readingListView;
    @BindView(R.id.empty_container) ViewGroup emptyContainer;
    @BindView(R.id.empty_title) TextView emptyTitle;
    @BindView(R.id.empty_message) TextView emptyMessage;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.reading_list_onboarding_container) ViewGroup onboardingContainer;
    @BindView(R.id.reading_list_swipe_refresh) SwipeRefreshLayout swipeRefreshLayout;

    private List<Object> displayedLists = new ArrayList<>();

    private ReadingListsFunnel funnel = new ReadingListsFunnel();
    private CompositeDisposable disposables = new CompositeDisposable();

    private ReadingListAdapter adapter = new ReadingListAdapter();
    private ReadingListItemCallback readingListItemCallback = new ReadingListItemCallback();
    private ReadingListPageItemCallback readingListPageItemCallback = new ReadingListPageItemCallback();
    private ReadingListsSearchCallback searchActionModeCallback = new ReadingListsSearchCallback();
    @Nullable private ActionMode actionMode;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private OverflowCallback overflowCallback = new OverflowCallback();
    private String currentSearchQuery;
    private static final int SAVE_COUNT_LIMIT = 3;
    public static final int ARTICLE_ITEM_IMAGE_DIMENSION = 57;

    @NonNull public static ReadingListsFragment newInstance() {
        return new ReadingListsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reading_lists, container, false);
        unbinder = ButterKnife.bind(this, view);

        searchEmptyView.setEmptyText(R.string.search_reading_lists_no_results);
        readingListView.setLayoutManager(new LinearLayoutManager(getContext()));
        readingListView.setAdapter(adapter);
        readingListView.addItemDecoration(new DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, false));
        readingListView.addItemDecoration(new MarginItemDecoration(0, 0, 0, DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.floating_queue_container_height))) {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                if (parent.getChildAdapterPosition(view) == adapter.getItemCount() - 1
                        && ((MainActivity) requireActivity()).isFloatingQueueEnabled()
                        && displayedLists.size() > 1) {
                    super.getItemOffsets(outRect, view, parent, state);
                }
            }
        });

        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));
        swipeRefreshLayout.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(() -> refreshSync(ReadingListsFragment.this, swipeRefreshLayout));
        if (ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            swipeRefreshLayout.setEnabled(false);
        }

        enableLayoutTransition(true);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        readingListView.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLists();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_reading_lists, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_lists:
                ((AppCompatActivity) requireActivity())
                        .startSupportActionMode(searchActionModeCallback);
                return true;
            case R.id.menu_overflow_button:
                ReadingListsOverflowView overflowView = new ReadingListsOverflowView(requireContext());
                overflowView.show(((MainActivity) requireActivity()).getToolbar().findViewById(R.id.menu_overflow_button), overflowCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onToggleItemOffline(@NonNull ReadingListPage page) {
        ReadingListBehaviorsUtil.INSTANCE.togglePageOffline(requireActivity(), page, this::updateLists);
    }

    @Override
    public void onShareItem(@NonNull ReadingListPage page) {
        ShareUtil.shareText(getContext(), ReadingListPage.toPageTitle(page));
    }

    @Override
    public void onAddItemToOther(@NonNull ReadingListPage page) {
        bottomSheetPresenter.show(getChildFragmentManager(),
                AddToReadingListDialog.newInstance(ReadingListPage.toPageTitle(page),
                        AddToReadingListDialog.InvokeSource.READING_LIST_ACTIVITY));
    }

    @Override
    public void onDeleteItem(@NonNull ReadingListPage page) {
        ReadingListBehaviorsUtil.INSTANCE.deletePages(requireActivity(), ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page), page, this::updateLists, this::updateLists);
    }

    private class OverflowCallback implements ReadingListsOverflowView.Callback {
        @Override
        public void sortByClick() {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    SortReadingListsDialog.newInstance(Prefs.getReadingListSortMode(SORT_BY_NAME_ASC)));
        }

        @Override
        public void createNewListClick() {
            String title = getString(R.string.reading_list_name_sample);
            List<String> existingTitles = new ArrayList<>();
            for (Object list : displayedLists) {
                if (list instanceof ReadingList) {
                    existingTitles.add(((ReadingList) list).title());
                }
            }
            ReadingListTitleDialog.readingListTitleDialog(requireContext(), title, "",
                    existingTitles, (text, description) -> {
                        ReadingListDbHelper.instance().createList(text, description);
                        updateLists();
                    }).show();
        }

        @Override
        public void refreshClick() {
            swipeRefreshLayout.setRefreshing(true);
            refreshSync(ReadingListsFragment.this, swipeRefreshLayout);
        }
    }

    private void sortListsBy(int option) {
        switch (option) {
            case SORT_BY_NAME_DESC:
                Prefs.setReadingListSortMode(SORT_BY_NAME_DESC);
                break;
            case SORT_BY_RECENT_DESC:
                Prefs.setReadingListSortMode(SORT_BY_RECENT_DESC);
                break;
            case SORT_BY_RECENT_ASC:
                Prefs.setReadingListSortMode(SORT_BY_RECENT_ASC);
                break;
            case SORT_BY_NAME_ASC:
            default:
                Prefs.setReadingListSortMode(SORT_BY_NAME_ASC);
                break;
        }
        updateLists();
    }

    public static void refreshSync(@NonNull Fragment fragment, @NonNull SwipeRefreshLayout swipeRefreshLayout) {
        if (!AccountUtil.isLoggedIn()) {
            ReadingListSyncBehaviorDialogs.promptLogInToSyncDialog(fragment.requireActivity());
            swipeRefreshLayout.setRefreshing(false);
        } else {
            Prefs.setReadingListSyncEnabled(true);
            ReadingListSyncAdapter.manualSyncWithRefresh();
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (!isAdded()) {
            return;
        }
        if (visible) {
            updateLists();
            maybeShowOnboarding();
        } else if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void enableLayoutTransition(boolean enable) {
        if (enable) {
            contentContainer.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            emptyContainer.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
            ((ViewGroup) emptyContainer.getChildAt(0)).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        } else {
            contentContainer.getLayoutTransition().disableTransitionType(LayoutTransition.CHANGING);
            emptyContainer.getLayoutTransition().disableTransitionType(LayoutTransition.CHANGING);
            ((ViewGroup) emptyContainer.getChildAt(0)).getLayoutTransition().disableTransitionType(LayoutTransition.CHANGING);
        }
    }

    private void updateLists() {
        updateLists(currentSearchQuery, !TextUtils.isEmpty(currentSearchQuery));
    }

    private void updateLists(@Nullable final String searchQuery, boolean forcedRefresh) {
        maybeShowOnboarding();
        ReadingListBehaviorsUtil.INSTANCE.searchListsAndPages(searchQuery, lists -> {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return lists.size();
                }
                @Override
                public int getNewListSize() {
                    return lists.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    if (displayedLists.size() <= oldItemPosition || lists.size() <= newItemPosition) {
                        return false;
                    }
                    return displayedLists.get(oldItemPosition) instanceof ReadingList && lists.get(newItemPosition) instanceof ReadingList
                            && ((ReadingList) displayedLists.get(oldItemPosition)).id() == ((ReadingList) lists.get(newItemPosition)).id();
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    if (displayedLists.size() <= oldItemPosition || lists.size() <= newItemPosition) {
                        return false;
                    }
                    return displayedLists.get(oldItemPosition) instanceof ReadingList && lists.get(newItemPosition) instanceof ReadingList
                            && ((ReadingList) displayedLists.get(oldItemPosition)).id() == ((ReadingList) lists.get(newItemPosition)).id()
                            && ((ReadingList) displayedLists.get(oldItemPosition)).pages().size() == ((ReadingList) lists.get(newItemPosition)).pages().size()
                            && ((ReadingList) displayedLists.get(oldItemPosition)).numPagesOffline() == ((ReadingList) lists.get(newItemPosition)).numPagesOffline();
                }
            });
            // If the number of lists has changed, just invalidate everything, as a
            // simple way to get the bottom item margin to apply to the correct item.
            boolean invalidateAll = forcedRefresh
                    || displayedLists.size() != lists.size()
                    || (!TextUtils.isEmpty(currentSearchQuery)
                    && !TextUtils.isEmpty(searchQuery)
                    && !currentSearchQuery.equals(searchQuery));
            displayedLists = lists;
            if (invalidateAll) {
                adapter.notifyDataSetChanged();
            } else {
                result.dispatchUpdatesTo(adapter);
            }

            swipeRefreshLayout.setRefreshing(false);
            maybeShowListLimitMessage();
            updateEmptyState(searchQuery);
            maybeDeleteListFromIntent();
            currentSearchQuery = searchQuery;
        });
    }

    private void maybeShowListLimitMessage() {
        if (getUserVisibleHint() && displayedLists.size() >= Constants.MAX_READING_LISTS_LIMIT) {
            String message = getString(R.string.reading_lists_limit_message);
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
        }
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(View.GONE);
            if (displayedLists.size() == 1) {
                setEmptyContainerVisibility(true);
                setUpEmptyContainer();
            }
            setEmptyContainerVisibility(displayedLists.size() == 1);
        } else {
            searchEmptyView.setVisibility(displayedLists.isEmpty() ? View.VISIBLE : View.GONE);
            setEmptyContainerVisibility(false);
        }
        contentContainer.setVisibility(displayedLists.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setEmptyContainerVisibility(boolean visible) {
        if (visible) {
            emptyContainer.setVisibility(View.VISIBLE);
            requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        } else {
            emptyContainer.setVisibility(View.GONE);
            DeviceUtil.setWindowSoftInputModeResizable(requireActivity());
        }
    }

    private void setUpEmptyContainer() {
        if (displayedLists.get(0) instanceof ReadingList && !((ReadingList) displayedLists.get(0)).pages().isEmpty()) {
            emptyTitle.setText(getString(R.string.no_user_lists_title));
            emptyMessage.setText(getString(R.string.no_user_lists_msg));
        } else {
            emptyTitle.setText(getString(R.string.saved_list_empty_title));
            emptyMessage.setText(getString(R.string.reading_lists_empty_message));
        }
    }

    @Override
    public void onSortOptionClick(int position) {
        sortListsBy(position);
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

    private class ReadingListPageItemHolder extends DefaultViewHolder<PageItemView<ReadingListPage>> {

        ReadingListPageItemHolder(PageItemView<ReadingListPage> itemView) {
            super(itemView);
        }

        void bindItem(ReadingListPage page) {
            getView().setItem(page);
            getView().setTitle(page.title());
            getView().setTitleMaxLines(2);
            getView().setTitleEllipsis();
            getView().setDescription(StringUtils.capitalize(page.description()));
            getView().setDescriptionMaxLines(2);
            getView().setDescriptionEllipsis();
            getView().setImageUrl(page.thumbUrl());
            getView().setListItemImageDimensions(DimenUtil.roundedDpToPx(ARTICLE_ITEM_IMAGE_DIMENSION), DimenUtil.roundedDpToPx(ARTICLE_ITEM_IMAGE_DIMENSION));
            getView().setSelected(page.selected());
            getView().setActionIcon(R.drawable.ic_more_vert_white_24dp);
            getView().setActionTint(R.attr.secondary_text_color);
            getView().setActionHint(R.string.abc_action_menu_overflow_description);
            getView().setSecondaryActionIcon(page.saving() ? R.drawable.ic_download_in_progress : R.drawable.ic_download_circle_gray_24dp,
                    !page.offline() || page.saving());
            getView().setCircularProgressVisibility(page.downloadProgress() > 0 && page.downloadProgress() < MAX_PROGRESS);
            getView().setProgress(page.downloadProgress() == MAX_PROGRESS ? 0 : page.downloadProgress());
            getView().setSecondaryActionHint(R.string.reading_list_article_make_offline);
            getView().setSearchQuery(currentSearchQuery);
            getView().setUpChipGroup(ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page));
            PageAvailableOfflineHandler.INSTANCE.check(page, available -> getView().setViewsGreyedOut(!available));
        }
    }

    private final class ReadingListAdapter extends RecyclerView.Adapter<DefaultViewHolder> {
        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_PAGE_ITEM = 1;

        @Override
        public int getItemViewType(int position) {
            if (displayedLists.get(position) instanceof ReadingList) {
                return VIEW_TYPE_ITEM;
            } else {
                return VIEW_TYPE_PAGE_ITEM;
            }
        }

        @Override
        public int getItemCount() {
            return displayedLists.size();
        }

        @Override
        public DefaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ITEM) {
                ReadingListItemView view = new ReadingListItemView(getContext());
                return new ReadingListItemHolder(view);
            } else {
                return new ReadingListPageItemHolder(new PageItemView<>(requireContext()));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder holder, int pos) {
            if (holder instanceof ReadingListItemHolder) {
                ((ReadingListItemHolder) holder).bindItem((ReadingList) displayedLists.get(pos));
            } else {
                ((ReadingListPageItemHolder) holder).bindItem((ReadingListPage) displayedLists.get(pos));
            }
        }

        @Override public void onViewAttachedToWindow(@NonNull DefaultViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof ReadingListItemHolder) {
                ((ReadingListItemHolder) holder).getView().setCallback(readingListItemCallback);
            } else {
                ((ReadingListPageItemHolder) holder).getView().setCallback(readingListPageItemCallback);
            }
        }

        @Override public void onViewDetachedFromWindow(@NonNull DefaultViewHolder holder) {
            if (holder instanceof ReadingListItemHolder) {
                ((ReadingListItemHolder) holder).getView().setCallback(null);
            } else {
                ((ReadingListPageItemHolder) holder).getView().setCallback(null);
            }
            super.onViewDetachedFromWindow(holder);
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
            if (readingList.isDefault()) {
                L.w("Attempted to rename default list.");
                return;
            }
            List<String> existingTitles = new ArrayList<>();
            for (Object list : displayedLists) {
                if (list instanceof ReadingList) {
                    existingTitles.add(((ReadingList) list).title());
                }
            }
            existingTitles.remove(readingList.title());
            ReadingListTitleDialog.readingListTitleDialog(requireContext(), readingList.title(),
                    readingList.description(), existingTitles, (text, description) -> {
                        readingList.title(text);
                        readingList.description(description);
                        readingList.dirty(true);
                        ReadingListDbHelper.instance().updateList(readingList, true);
                        ReadingListSyncAdapter.manualSync();

                        updateLists();
                        funnel.logModifyList(readingList, displayedLists.size());
                    }).show();
            ReadingListBehaviorsUtil.INSTANCE.renameReadingList(requireActivity(), readingList, () -> {
                updateLists(currentSearchQuery, true);
                funnel.logModifyList(readingList, displayedLists.size());
            });
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.deleteReadingList(requireActivity(), readingList, true, () -> {
                ReadingListBehaviorsUtil.INSTANCE.showDeleteListUndoSnackbar(requireActivity(), readingList, ReadingListsFragment.this::updateLists);
                funnel.logDeleteList(readingList, displayedLists.size());
                updateLists();
            });
        }

        @Override
        public void onSaveAllOffline(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.savePagesForOffline(requireActivity(), readingList.pages(), ReadingListsFragment.this::updateLists);
        }

        @Override
        public void onRemoveAllOffline(@NonNull ReadingList readingList) {
            ReadingListBehaviorsUtil.INSTANCE.removePagesFromOffline(requireActivity(), readingList.pages(), ReadingListsFragment.this::updateLists);
        }
    }


    private class ReadingListPageItemCallback implements PageItemView.Callback<ReadingListPage> {

        @Override
        public void onClick(@Nullable ReadingListPage page) {
            if (page != null) {
                PageTitle title = ReadingListPage.toPageTitle(page);
                HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);

                page.touch();
                Completable.fromAction(() -> {
                    ReadingListDbHelper.instance().updateLists(ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page), false);
                    ReadingListDbHelper.instance().updatePage(page);
                }).subscribeOn(Schedulers.io()).subscribe();

                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.getTitle()));
            }
        }

        @Override
        public boolean onLongClick(@Nullable ReadingListPage item) {
            return false;
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
                    ReadingListItemActionsDialog.newInstance(ReadingListBehaviorsUtil.INSTANCE.getListsContainPage(page), page));
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
                    });
                }
            }
        }

        @Override
        public void onListChipClick(@Nullable ReadingList readingList) {
            startActivity(ReadingListActivity.newIntent(requireContext(), readingList));
        }
    }

    private void maybeDeleteListFromIntent() {
        if (requireActivity().getIntent().hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            String titleToDelete = requireActivity().getIntent()
                    .getStringExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST);
            requireActivity().getIntent().removeExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST);
            for (Object list : displayedLists) {
                if (list instanceof ReadingList && ((ReadingList) list).title().equals(titleToDelete)) {
                    ReadingListBehaviorsUtil.INSTANCE.deleteReadingList(requireActivity(), ((ReadingList) list), false, () -> {
                        ReadingListBehaviorsUtil.INSTANCE.showDeleteListUndoSnackbar(requireActivity(), ((ReadingList) list), this::updateLists);
                        funnel.logDeleteList(((ReadingList) list), displayedLists.size());
                        updateLists();
                    });
                }
            }
        }
    }

    private class ReadingListsSearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            // searching delay will let the animation cannot catch the update of list items, and will cause crashes
            enableLayoutTransition(false);
            ViewUtil.finishActionModeWhenTappingOnView(getView(), actionMode);
            ViewUtil.finishActionModeWhenTappingOnView(emptyContainer, actionMode);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            String searchString = s.trim();
            ((MainFragment) getParentFragment())
                    .setBottomNavVisible(searchString.length() == 0);
            updateLists(searchString, false);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            enableLayoutTransition(true);
            actionMode = null;
            currentSearchQuery = null;
            updateLists();
        }

        @Override
        protected String getSearchHintString() {
            return requireContext().getResources().getString(R.string.search_hint_search_my_lists_and_articles);
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

    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ReadingListSyncEvent) {
                readingListView.post(() -> {
                    if (isAdded()) {
                        updateLists();
                    }
                });
            } else if (event instanceof ArticleSavedOrDeletedEvent) {
                if (((ArticleSavedOrDeletedEvent) event).isAdded()) {
                    if (Prefs.getReadingListsPageSaveCount() < SAVE_COUNT_LIMIT) {
                        showReadingListsSyncDialog();
                        Prefs.setReadingListsPageSaveCount(Prefs.getReadingListsPageSaveCount() + 1);
                    }
                }
            }
        }
    }

    private void showReadingListsSyncDialog() {
        if (!Prefs.isReadingListSyncEnabled()) {
            if (AccountUtil.isLoggedIn()) {
                ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(requireActivity());
            } else {
                ReadingListSyncBehaviorDialogs.promptLogInToSyncDialog(requireActivity());
            }
        }
    }

    private void maybeShowOnboarding() {
        onboardingContainer.removeAllViews();

        if (AccountUtil.isLoggedIn() && !Prefs.isReadingListSyncEnabled()
                && Prefs.isReadingListSyncReminderEnabled()
                && !ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            OnboardingView onboardingView = new OnboardingView(requireContext());
            onboardingView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.base20));
            onboardingView.setTitle(R.string.reading_lists_sync_reminder_title);
            onboardingView.setText(StringUtil.fromHtml(getString(R.string.reading_lists_sync_reminder_text)));
            onboardingView.setPositiveAction(R.string.reading_lists_sync_reminder_action);
            onboardingContainer.addView(onboardingView);
            onboardingView.setCallback(new SyncReminderOnboardingCallback());

        } else if (!AccountUtil.isLoggedIn() && Prefs.isReadingListLoginReminderEnabled()
                && !ReadingListSyncAdapter.isDisabledByRemoteConfig()) {
            OnboardingView onboardingView = new OnboardingView(requireContext());
            onboardingView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.base20));
            onboardingView.setTitle(R.string.reading_list_login_reminder_title);
            onboardingView.setText(R.string.reading_lists_login_reminder_text);
            onboardingView.setNegativeAction(R.string.reading_lists_onboarding_got_it);
            onboardingView.setPositiveAction(R.string.reading_lists_sync_login);
            onboardingContainer.addView(onboardingView);
            onboardingView.setCallback(new LoginReminderOnboardingCallback());
        }
    }

    private class SyncReminderOnboardingCallback implements OnboardingView.Callback {
        @Override
        public void onPositiveAction() {
            Prefs.shouldShowReadingListSyncMergePrompt(true);
            ReadingListSyncAdapter.setSyncEnabledWithSetup();
            maybeShowOnboarding();
        }

        @Override
        public void onNegativeAction() {
            Prefs.setReadingListSyncReminderEnabled(false);
            maybeShowOnboarding();
        }
    }

    private class LoginReminderOnboardingCallback implements OnboardingView.Callback {
        @Override
        public void onPositiveAction() {
            if (getParentFragment() instanceof FeedFragment.Callback) {
                ((FeedFragment.Callback) getParentFragment()).onLoginRequested();
            }
        }

        @Override
        public void onNegativeAction() {
            Prefs.setReadingListLoginReminderEnabled(false);
            maybeShowOnboarding();
        }
    }
}
