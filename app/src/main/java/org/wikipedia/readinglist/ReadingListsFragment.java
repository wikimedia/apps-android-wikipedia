package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;

import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class ReadingListsFragment extends Fragment implements BackPressedHandler {
    private static final int PAGE_READING_LISTS = 0;
    private static final int PAGE_LIST_DETAIL = 1;

    private RecyclerView readingListView;
    private View emptyContainer;
    private ViewPager pager;
    private List<ReadingList> readingLists = new ArrayList<>();
    private ReadingListsFunnel funnel = new ReadingListsFunnel();

    private ReadingListDetailView listDetailView;
    private ReadingListAdapter adapter = new ReadingListAdapter();
    private ReadingListPagerAdapter pagerAdapter = new ReadingListPagerAdapter();

    private ReadingListItemActionListener itemActionListener = new ReadingListItemActionListener();
    private ReadingListActionListener actionListener = new ReadingListActionListener();

    private int readingListSortMode;
    private int readingListPageSortMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        readingListSortMode = Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC);
        readingListPageSortMode = Prefs.getReadingListPageSortMode(ReadingList.SORT_BY_NAME_ASC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_reading_lists, container, false);
        rootView.setPadding(0, getContentTopOffsetPx(getActivity()), 0, 0);
        readingListView = (RecyclerView) rootView.findViewById(R.id.reading_list_list);
        emptyContainer = rootView.findViewById(R.id.empty_container);

        pager = (ViewPager) rootView.findViewById(R.id.pager);
        listDetailView = (ReadingListDetailView) rootView.findViewById(R.id.list_detail_view);
        listDetailView.setActionListener(actionListener);
        listDetailView.setOnItemActionListener(itemActionListener);

        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        readingListView.setAdapter(adapter);

        pager = (ViewPager) rootView.findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                getActivity().supportInvalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        updateLists();
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        readingListView.setAdapter(null);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_reading_lists, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem sortByNameItem = menu.findItem(R.id.menu_sort_by_name);
        MenuItem sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent);
        if (pager.getCurrentItem() == PAGE_READING_LISTS) {
            sortByNameItem.setTitle(readingListSortMode == ReadingList.SORT_BY_NAME_ASC ? R.string.reading_list_sort_by_name_desc : R.string.reading_list_sort_by_name);
            sortByRecentItem.setTitle(readingListSortMode == ReadingList.SORT_BY_RECENT_DESC ? R.string.reading_list_sort_by_recent_desc : R.string.reading_list_sort_by_recent);
        } else {
            sortByNameItem.setTitle(readingListPageSortMode == ReadingList.SORT_BY_NAME_ASC ? R.string.reading_list_sort_by_name_desc : R.string.reading_list_sort_by_name);
            sortByRecentItem.setTitle(readingListPageSortMode == ReadingList.SORT_BY_RECENT_DESC ? R.string.reading_list_sort_by_recent_desc : R.string.reading_list_sort_by_recent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_name:
                setSortMode(ReadingList.SORT_BY_NAME_ASC, ReadingList.SORT_BY_NAME_DESC);
                return true;
            case R.id.menu_sort_by_recent:
                setSortMode(ReadingList.SORT_BY_RECENT_DESC, ReadingList.SORT_BY_RECENT_ASC);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateLists() {
        ReadingList.DAO.queryMruLists(new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> rows) {
                readingLists = rows;
                sortLists();
                updateEmptyMessage();
            }
        });
    }

    private void updateEmptyMessage() {
        emptyContainer.setVisibility(readingLists.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class ReadingListActionListener implements ReadingListDetailView.ReadingListActionListener {
        @Override
        public void onUpdate(ReadingList readingList, String newTitle, String newDescription, boolean saveOffline) {
            readingList.setDescription(newDescription);
            readingList.setSaveOffline(saveOffline);
            ReadingList.DAO.renameAndSaveListInfo(readingList, newTitle);
            funnel.logModifyList(readingList, readingLists.size());
        }

        @Override
        public void onDelete(ReadingList readingList) {
            showDeleteListUndoSnackbar(readingList);
            ReadingList.DAO.removeList(readingList);
            funnel.logDeleteList(readingList, readingLists.size());
            pager.setCurrentItem(PAGE_READING_LISTS);
            updateLists();
        }
    }

    class ReadingListPagerAdapter extends PagerAdapter {
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

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
    }

    private class ReadingListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ReadingListItemView itemView;
        private ReadingList readingList;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setOnClickListener(this);
        }

        public void bindItem(ReadingList readingList) {
            this.readingList = readingList;
            itemView.setReadingList(readingList);
        }

        @Override
        public void onClick(View v) {
            listDetailView.setReadingList(readingList);
            listDetailView.setSort(readingListPageSortMode);
            pager.setCurrentItem(PAGE_LIST_DETAIL);

            if (!readingList.getPages().isEmpty()
                    && Prefs.isReadingListPageDeleteTutorialEnabled()) {
                showDeletePageOnboarding();
            }
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
    }

    private class ReadingListItemActionListener implements ReadingListDetailView.ReadingListItemActionListener {
        @Override
        public void onClick(ReadingList readingList, ReadingListPage page) {
            PageTitle title = ReadingListDaoProxy.pageTitle(page);
            HistoryEntry newEntry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);
            ((MainActivity) getActivity()).loadPage(title, newEntry);

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
    }

    private void showDeleteListUndoSnackbar(final ReadingList readingList) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getView(),
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
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getView(),
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
        FeedbackUtil.makeSnackbar(getView(), getString(R.string.reading_lists_onboarding_page_delete), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.reading_lists_onboarding_got_it, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Prefs.setReadingListPageDeleteTutorialEnabled(false);
                    }
                }).show();
    }

    private void setSortMode(int sortModeAsc, int sortModeDesc) {
        if (pager.getCurrentItem() == PAGE_READING_LISTS) {
            if (readingListSortMode != sortModeAsc) {
                readingListSortMode = sortModeAsc;
            } else {
                readingListSortMode = sortModeDesc;
            }
            sortLists();
            Prefs.setReadingListSortMode(readingListSortMode);
        } else if (pager.getCurrentItem() == PAGE_LIST_DETAIL) {
            if (readingListPageSortMode != sortModeAsc) {
                readingListPageSortMode = sortModeAsc;
            } else {
                readingListPageSortMode = sortModeDesc;
            }
            listDetailView.setSort(readingListPageSortMode);
            Prefs.setReadingListPageSortMode(readingListPageSortMode);
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    private void sortLists() {
        ReadingList.sortReadingLists(readingLists, readingListSortMode);
        adapter.notifyDataSetChanged();
    }
}
