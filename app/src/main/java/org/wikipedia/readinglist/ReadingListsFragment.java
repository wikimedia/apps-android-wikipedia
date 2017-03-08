package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import android.widget.ImageView;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.TextInputDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import butterknife.Unbinder;

public class ReadingListsFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onLoadPage(PageTitle title, HistoryEntry entry);
    }

    private static final int PAGE_READING_LISTS = 0;
    private static final int PAGE_LIST_DETAIL = 1;

    private Unbinder unbinder;
    @BindView(R.id.reading_list_list) RecyclerView readingListView;
    @BindView(R.id.empty_container) View emptyContainer;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.pager) ViewPager pager;
    private ReadingLists readingLists = new ReadingLists();
    private ReadingListsFunnel funnel = new ReadingListsFunnel();

    @BindView(R.id.list_detail_view) ReadingListDetailView listDetailView;
    private ImageView detailViewBackButton;
    private ReadingListAdapter adapter = new ReadingListAdapter();
    private ReadingListPagerAdapter pagerAdapter = new ReadingListPagerAdapter();

    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();
    private ReadingListDetailViewCallback detailViewCallback = new ReadingListDetailViewCallback();
    private ReadingListsSearchCallback searchActionModeCallback = new ReadingListsSearchCallback();
    @Nullable private ActionMode actionMode;

    @NonNull public static ReadingListsFragment newInstance() {
        return new ReadingListsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WikipediaApp.getInstance().getRefWatcher().watch(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reading_lists, container, false);
        unbinder = ButterKnife.bind(this, view);

        searchEmptyView.setEmptyText(R.string.search_reading_lists_no_results);
        detailViewBackButton = (ImageView) listDetailView.findViewById(R.id.reading_list_detail_back_button);
        listDetailView.setCallback(detailViewCallback);

        readingListView.setLayoutManager(new LinearLayoutManager(getContext()));
        readingListView.setAdapter(adapter);
        readingListView.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));

        pager.setAdapter(pagerAdapter);

        updateLists();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        listDetailView.setCallback(null);
        readingListView.setAdapter(null);
        pager.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public boolean onBackPressed() {
        if (pager.getCurrentItem() != PAGE_READING_LISTS) {
            pager.setCurrentItem(PAGE_READING_LISTS);
            updateLists();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        pager.setCurrentItem(PAGE_READING_LISTS);
        updateLists();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_reading_lists, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem sortByNameItem = menu.findItem(R.id.menu_sort_by_name);
        MenuItem sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent);
        int sortMode = pager.getCurrentItem() == PAGE_READING_LISTS
                ? Prefs.getReadingListSortMode(ReadingLists.SORT_BY_NAME_ASC)
                : Prefs.getReadingListPageSortMode(ReadingLists.SORT_BY_NAME_ASC);
        sortByNameItem.setTitle(sortMode == ReadingLists.SORT_BY_NAME_ASC ? R.string.reading_list_sort_by_name_desc : R.string.reading_list_sort_by_name);
        sortByRecentItem.setTitle(sortMode == ReadingLists.SORT_BY_RECENT_DESC ? R.string.reading_list_sort_by_recent_desc : R.string.reading_list_sort_by_recent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_name:
                setSortMode(ReadingLists.SORT_BY_NAME_ASC, ReadingLists.SORT_BY_NAME_DESC);
                return true;
            case R.id.menu_sort_by_recent:
                setSortMode(ReadingLists.SORT_BY_RECENT_DESC, ReadingLists.SORT_BY_RECENT_ASC);
                return true;
            case R.id.menu_search_lists:
                ((AppCompatActivity) getActivity())
                        .startSupportActionMode(searchActionModeCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setUserVisibleHint(boolean visible) {
        super.setUserVisibleHint(visible);
        if (!isAdded()) {
            return;
        }
        if (visible) {
            pager.setCurrentItem(PAGE_READING_LISTS);
            updateLists();
        } else if (actionMode != null) {
            actionMode.finish();
        }
    }

    @OnPageChange(R.id.pager) void onListChanged() {
        getActivity().supportInvalidateOptionsMenu();
    }

    private void updateLists() {
        updateLists(null);
    }

    private void updateLists(@Nullable final String searchQuery) {
        ReadingList.DAO.queryMruLists(searchQuery,
                new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> rows) {
                readingLists.set(rows);
                sortLists();
                updateEmptyState(searchQuery);
            }
        });
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(View.GONE);
            emptyContainer.setVisibility(readingLists.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            searchEmptyView.setVisibility(readingLists.isEmpty() ? View.VISIBLE : View.GONE);
            emptyContainer.setVisibility(View.GONE);
        }
    }

    private class ReadingListPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            int resId;
            switch (position) {
                case PAGE_LIST_DETAIL:
                    resId = R.id.list_detail_page;
                    break;
                case PAGE_READING_LISTS:
                default:
                    resId = R.id.list_of_lists_page;
                    break;
            }
            return getView().findViewById(resId);
        }

        @Override public void destroyItem(ViewGroup container, int position, Object object) { }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    private class ReadingListItemHolder extends RecyclerView.ViewHolder {
        private ReadingListItemView itemView;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        public void bindItem(ReadingList readingList) {
            itemView.setReadingList(readingList);
        }

        public ReadingListItemView getView() {
            return itemView;
        }
    }

    private final class ReadingListAdapter extends RecyclerView.Adapter<ReadingListItemHolder> {
        @Override
        public int getItemCount() {
            return readingLists.size();
        }

        @Override
        public ReadingListItemHolder onCreateViewHolder(ViewGroup parent, int pos) {
            ReadingListItemView view = new ReadingListItemView(getContext());
            return new ReadingListItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ReadingListItemHolder holder, int pos) {
            holder.bindItem(readingLists.get(pos));
        }

        @Override public void onViewAttachedToWindow(ReadingListItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(listItemCallback);
        }

        @Override public void onViewDetachedFromWindow(ReadingListItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ReadingListItemCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
            if (actionMode != null) {
                actionMode.finish();
            }
            startActivity(ReadingListActivity.newIntent(getContext(), readingList));
        }

        @Override
        public void onRename(@NonNull final ReadingList readingList) {
            ReadingListTitleDialog.readingListTitleDialog(getContext(), readingList.getTitle(),
                    readingLists.getTitlesExcept(readingList.getTitle()),
                    new ReadingListTitleDialog.Callback() {
                        @Override
                        public void onSuccess(@NonNull CharSequence text) {
                            ReadingList.DAO.renameAndSaveListInfo(readingList, text.toString());
                            updateLists();
                            funnel.logModifyList(readingList, readingLists.size());
                        }
                    }).show();
        }

        @Override
        public void onEditDescription(@NonNull final ReadingList readingList) {
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
                    updateLists();
                    funnel.logModifyList(readingList, readingLists.size());
                }
            }).show();
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
            showDeleteListUndoSnackbar(readingList);
            ReadingList.DAO.removeList(readingList);
            funnel.logDeleteList(readingList, readingLists.size());
            updateLists();
        }
    }

    private class ReadingListDetailViewCallback implements ReadingListDetailView.Callback {
        @Override
        public void onClick(ReadingList readingList, ReadingListPage page) {
            PageTitle title = ReadingListDaoProxy.pageTitle(page);
            HistoryEntry newEntry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);
            onPageClick(title, newEntry);

            ReadingList.DAO.makeListMostRecent(readingList);
        }

        @Override
        public void onLongClick(ReadingList readingList, ReadingListPage page) {
            // TODO: implement integration with PageLongPressHandler
        }

        @Override
        public void onDelete(ReadingList readingList, ReadingListPage page) {
            showDeleteItemUndoSnackbar(readingList, page);
            ReadingList.DAO.removeTitleFromList(readingList, page);
            funnel.logDeleteItem(readingList, readingLists.size());
            updateLists();
        }

        @Override
        public void onBackPressed() {
            ReadingListsFragment.this.onBackPressed();
        }
    }

    private void onPageClick(PageTitle title, HistoryEntry entry) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLoadPage(title, entry);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private void showDeleteListUndoSnackbar(final ReadingList readingList) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(),
                String.format(getString(R.string.reading_list_deleted), readingList.getTitle()),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReadingList.DAO.addList(readingList);
                updateLists();
            }
        });
        snackbar.show();
    }

    private void showDeleteItemUndoSnackbar(final ReadingList readingList, final ReadingListPage page) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getActivity(),
                String.format(getString(R.string.reading_list_item_deleted), page.title()),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReadingList.DAO.addTitleToList(readingList, page);
                ReadingListPageDao.instance().markOutdated(page);
                listDetailView.updateDetails();
            }
        });
        snackbar.show();
    }

    private void showDeletePageOnboarding() {
        FeedbackUtil.makeSnackbar(getActivity(), getString(R.string.reading_lists_onboarding_page_delete), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.reading_lists_onboarding_got_it, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Prefs.setReadingListPageDeleteTutorialEnabled(false);
                    }
                }).show();
    }

    private void setSortMode(int sortModeAsc, int sortModeDesc) {
        if (pager.getCurrentItem() == PAGE_READING_LISTS) {
            int sortMode = Prefs.getReadingListSortMode(ReadingLists.SORT_BY_NAME_ASC);
            if (sortMode != sortModeAsc) {
                sortMode = sortModeAsc;
            } else {
                sortMode = sortModeDesc;
            }
            Prefs.setReadingListSortMode(sortMode);
            sortLists();
        } else if (pager.getCurrentItem() == PAGE_LIST_DETAIL) {
            int sortMode = Prefs.getReadingListPageSortMode(ReadingLists.SORT_BY_NAME_ASC);
            if (sortMode != sortModeAsc) {
                sortMode = sortModeAsc;
            } else {
                sortMode = sortModeDesc;
            }
            Prefs.setReadingListPageSortMode(sortMode);
            listDetailView.updateSort();
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    private void sortLists() {
        readingLists.sort(Prefs.getReadingListSortMode(ReadingLists.SORT_BY_NAME_ASC));
        adapter.notifyDataSetChanged();
    }

    private class ReadingListsSearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            detailViewBackButton.setVisibility(View.INVISIBLE);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            if (pager.getCurrentItem() == PAGE_READING_LISTS) {
                updateLists(s);
            } else if (pager.getCurrentItem() == PAGE_LIST_DETAIL) {
                listDetailView.setSearchQuery(s);
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            actionMode = null;
            detailViewBackButton.setVisibility(View.VISIBLE);
            if (pager.getCurrentItem() == PAGE_READING_LISTS) {
                updateLists();
            } else if (pager.getCurrentItem() == PAGE_LIST_DETAIL) {
                listDetailView.setSearchQuery(null);
            }
        }

        @Override
        protected String getSearchHintString() {
            switch (pager.getCurrentItem()) {
                case PAGE_READING_LISTS:
                    return getContext().getResources().getString(R.string.search_hint_search_my_lists);
                case PAGE_LIST_DETAIL:
                    return getContext().getResources().getString(R.string.search_hint_search_reading_list);
                default:
                    throw new IllegalArgumentException("Received unknown page ID " + pager.getCurrentItem());
            }
        }
    }
}
