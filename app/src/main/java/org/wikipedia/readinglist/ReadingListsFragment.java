package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;

import java.util.ArrayList;
import java.util.List;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class ReadingListsFragment extends Fragment implements BackPressedHandler {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
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

        View sortButton = rootView.findViewById(R.id.button_sort);
        FeedbackUtil.setToolbarButtonLongPressToast(sortButton);

        updateLists();
        return rootView;
    }

    @Override
    public void onDestroyView() {
        readingListView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public boolean onBackPressed() {
        if (pager.getCurrentItem() > 0) {
            pager.setCurrentItem(0);
            return true;
        }
        return false;
    }

    private void updateLists() {
        readingLists = ReadingList.DAO.queryMruLists();
        adapter.notifyDataSetChanged();
        updateEmptyMessage();
    }

    private void updateEmptyMessage() {
        emptyContainer.setVisibility(readingLists.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class ReadingListActionListener implements ReadingListDetailView.ReadingListActionListener {
        @Override
        public void onUpdate(ReadingList readingList) {
            ReadingList.DAO.saveListInfo(readingList);
            funnel.logModifyList(readingList, readingLists.size());
        }

        @Override
        public void onDelete(ReadingList readingList) {
            showDeleteListUndoSnackbar(readingList);
            ReadingList.DAO.removeList(readingList);
            funnel.logDeleteList(readingList, readingLists.size());
            pager.setCurrentItem(0);
            updateLists();
        }
    }

    class ReadingListPagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            int resId = 0;
            switch (position) {
                case 0:
                    resId = R.id.list_of_lists_page;
                    break;
                case 1:
                    resId = R.id.list_detail_page;
                    break;
                default:
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
            pager.setCurrentItem(1);
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
        public void onClick(ReadingList readingList, PageTitle title) {
            HistoryEntry newEntry = new HistoryEntry(title, HistoryEntry.SOURCE_READING_LIST);
            ((PageActivity) getActivity()).loadPage(title, newEntry);

            ReadingList.DAO.makeListMostRecent(readingList);
        }

        @Override
        public void onLongClick(ReadingList readingList, PageTitle title) {
            // TODO: implement integration with PageLongPressHandler
        }

        @Override
        public void onDelete(ReadingList readingList, PageTitle title) {
            showDeleteItemUndoSnackbar(readingList, title);
            ReadingList.DAO.removeTitleFromList(readingList, title);
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

    private void showDeleteItemUndoSnackbar(final ReadingList readingList, final PageTitle title) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(getView(),
                String.format(getString(R.string.reading_list_item_deleted), title.getDisplayText()),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ReadingList.DAO.addTitleToList(readingList, title);
                listDetailView.updateDetails();
                adapter.notifyDataSetChanged();

            }
        });
        snackbar.show();
    }
}
