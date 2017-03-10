package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
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

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.TextInputDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ReadingListsFragment extends Fragment {
    private Unbinder unbinder;
    @BindView(R.id.reading_list_list) RecyclerView readingListView;
    @BindView(R.id.empty_container) View emptyContainer;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;

    private ReadingLists readingLists = new ReadingLists();
    private ReadingListsFunnel funnel = new ReadingListsFunnel();

    private ReadingListAdapter adapter = new ReadingListAdapter();
    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();
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
        readingListView.setLayoutManager(new LinearLayoutManager(getContext()));
        readingListView.setAdapter(adapter);
        readingListView.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));

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
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem sortByNameItem = menu.findItem(R.id.menu_sort_by_name);
        MenuItem sortByRecentItem = menu.findItem(R.id.menu_sort_by_recent);
        int sortMode = Prefs.getReadingListSortMode(ReadingLists.SORT_BY_NAME_ASC);
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
            updateLists();
        } else if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void updateLists() {
        updateLists(null);
    }

    private void updateLists(@Nullable final String searchQuery) {
        ReadingList.DAO.queryMruLists(searchQuery,
                new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> rows) {
                if (getActivity() == null) {
                    return;
                }
                readingLists.set(rows);
                sortLists();
                updateEmptyState(searchQuery);
                maybeDeleteListFromIntent();
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
            deleteList(readingList);
        }
    }

    private void maybeDeleteListFromIntent() {
        if (getActivity().getIntent().hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            String titleToDelete = getActivity().getIntent()
                    .getStringExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST);
            getActivity().getIntent().removeExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST);
            deleteList(readingLists.get(titleToDelete));
        }
    }

    private void deleteList(@Nullable ReadingList readingList) {
        if (readingList != null) {
            showDeleteListUndoSnackbar(readingList);
            ReadingList.DAO.removeList(readingList);
            funnel.logDeleteList(readingList, readingLists.size());
            updateLists();
        }
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

    private void setSortMode(int sortModeAsc, int sortModeDesc) {
        int sortMode = Prefs.getReadingListSortMode(ReadingLists.SORT_BY_NAME_ASC);
        if (sortMode != sortModeAsc) {
            sortMode = sortModeAsc;
        } else {
            sortMode = sortModeDesc;
        }
        Prefs.setReadingListSortMode(sortMode);
        sortLists();
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
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            updateLists(s);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            actionMode = null;
            updateLists();
        }

        @Override
        protected String getSearchHintString() {
            return getContext().getResources().getString(R.string.search_hint_search_my_lists);
        }
    }
}
