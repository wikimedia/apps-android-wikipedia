package org.wikipedia.readinglist;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.PageItemView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.readinglist.ReadingListActivity.EXTRA_READING_LIST_TITLE;

public class ReadingListFragment extends Fragment {
    @BindView(R.id.reading_list_toolbar) Toolbar toolbar;
    @BindView(R.id.reading_list_toolbar_container) CollapsingToolbarLayout toolBarLayout;
    @BindView(R.id.reading_list_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.reading_list_contents) RecyclerView recyclerView;
    private Unbinder unbinder;

    @Nullable private ReadingList readingList;
    private ReadingListPageItemAdapter adapter = new ReadingListPageItemAdapter();
    @Nullable private ActionMode actionMode;
    private AppBarListener appBarListener = new AppBarListener();
    private boolean showOverflowMenu = false;

    @NonNull private ReadingLists readingLists = new ReadingLists();
    private SearchCallback searchActionModeCallback = new SearchCallback();

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));

        final String readingListTitle = getArguments().getString(EXTRA_READING_LIST_TITLE);
        ReadingList.DAO.queryMruLists(null, new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> lists) {
                if (getActivity() == null) {
                    return;
                }
                readingLists.set(lists);
                readingList = readingLists.get(readingListTitle);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_lists:
                getAppCompatActivity().startSupportActionMode(searchActionModeCallback);
                return true;
            case R.id.menu_sort_by_name:
                return true;
            case R.id.menu_sort_by_recent:
                return true;
            case R.id.menu_reading_list_rename:
                return true;
            case R.id.menu_reading_list_edit_description:
                return true;
            case R.id.menu_reading_list_delete:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void update() {
        setSearchQuery(currentSearchQuery);
    }

    private void setSearchQuery(@Nullable String query) {
        if (readingList == null) {
            return;
        }
        currentSearchQuery = query;
        displayedPages.clear();
        adapter.notifyDataSetChanged();
        if (TextUtils.isEmpty(query)) {
            displayedPages.addAll(readingList.getPages());
            return;
        }
        query = query.toUpperCase();
        for (ReadingListPage page : readingList.getPages()) {
            if (page.title().toUpperCase().contains(query.toUpperCase())) {
                displayedPages.add(page);
            }
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

    private class ReadingListPageItemHolder extends DefaultViewHolder<PageItemView<ReadingListPage>> {
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
        }
    }

    private final class ReadingListPageItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return displayedPages.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new ReadingListPageItemHolder(new PageItemView<ReadingListPage>(getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
            if (readingList != null && holder instanceof ReadingListPageItemHolder) {
                ((ReadingListPageItemHolder) holder).bindItem(displayedPages.get(pos));
            }
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
}
