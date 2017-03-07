package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.concurrency.CallbackTask;

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
    @NonNull private ReadingLists readingLists = new ReadingLists();

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

        final String readingListTitle = getArguments().getString(EXTRA_READING_LIST_TITLE);
        ReadingList.DAO.queryMruLists(null, new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> lists) {
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
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void update() {
        // TODO: update UI state for the current reading list.
    }
}
