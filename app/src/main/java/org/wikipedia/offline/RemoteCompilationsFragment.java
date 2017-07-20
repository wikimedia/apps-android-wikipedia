package org.wikipedia.offline;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.views.SearchEmptyView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RemoteCompilationsFragment extends Fragment {
    @BindView(R.id.compilation_list_toolbar_container) CollapsingToolbarLayout toolbarLayout;
    @BindView(R.id.compilation_list_toolbar) Toolbar toolbar;
    @BindView(R.id.compilation_list_contents) RecyclerView recyclerView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    private Unbinder unbinder;

    @NonNull
    public static RemoteCompilationsFragment newInstance() {
        RemoteCompilationsFragment instance = new RemoteCompilationsFragment();
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_remote_compilations, container, false);
        unbinder = ButterKnife.bind(this, view);

        toolbarLayout.setExpandedTitleColor(Color.WHITE);
        toolbarLayout.setCollapsedTitleTextColor(Color.WHITE);

        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_local_compilations, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search_compilations:
                // TODO
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }
}
