package org.wikipedia.offline;

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
import android.widget.ProgressBar;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RemoteCompilationsFragment extends Fragment {
    @BindView(R.id.compilation_list_toolbar_container) CollapsingToolbarLayout toolbarLayout;
    @BindView(R.id.compilation_list_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.compilation_list_toolbar) Toolbar toolbar;
    @BindView(R.id.compilation_list_contents) RecyclerView recyclerView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.compilation_list_progress) ProgressBar progressBar;
    @BindView(R.id.compilation_list_error) WikiErrorView errorView;
    private Unbinder unbinder;

    private boolean updating;
    private Throwable lastError;
    private CompilationCallback compilationCallback = new CompilationCallback();
    private CompilationItemAdapter adapter = new CompilationItemAdapter();
    private ItemCallback itemCallback = new ItemCallback();

    private SearchCallback searchActionModeCallback = new SearchCallback();
    @NonNull private List<Compilation> allItems = new ArrayList<>();
    @NonNull private List<Compilation> displayedItems = new ArrayList<>();
    private String currentSearchQuery;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));

        errorView.setRetryClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginUpdate();
            }
        });

        errorView.setBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        beginUpdate();

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
                ((AppCompatActivity) getActivity()).startSupportActionMode(searchActionModeCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void beginUpdate() {
        updating = true;
        lastError = null;
        new CompilationClient().request(WikipediaApp.getInstance().getWikiSite(), compilationCallback);
        updateEmptyState();
    }

    private void setSearchQuery(@Nullable String query) {
        currentSearchQuery = query;
        displayedItems.clear();
        if (TextUtils.isEmpty(query)) {
            displayedItems.addAll(allItems);
        } else {
            query = query.toUpperCase();
            for (Compilation c : allItems) {
                if (c.name().toUpperCase().contains(query.toUpperCase())) {
                    displayedItems.add(c);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState(query);
    }

    private void updateEmptyState() {
        updateEmptyState(currentSearchQuery);
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        progressBar.setVisibility(updating ? View.VISIBLE : View.GONE);
        if (lastError != null) {
            errorView.setError(lastError);
            errorView.setVisibility(View.VISIBLE);
            searchEmptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        errorView.setVisibility(View.GONE);
        if (TextUtils.isEmpty(searchQuery)) {
            searchEmptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(displayedItems.isEmpty() ? View.GONE : View.VISIBLE);
            searchEmptyView.setVisibility(displayedItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private class CompilationItemHolder extends DefaultViewHolder<PageItemView<Compilation>> {
        private Compilation compilation;

        CompilationItemHolder(PageItemView<Compilation> itemView) {
            super(itemView);
        }

        void bindItem(Compilation compilation) {
            this.compilation = compilation;
            getView().setItem(compilation);
            getView().setTitle(compilation.name());
            getView().setDescription(compilation.description());
            getView().setImageUrl(compilation.thumbUri() == null ? null : compilation.thumbUri().toString());
            getView().setActionIcon(R.drawable.ic_more_vert_white_24dp);
            getView().setActionHint(R.string.abc_action_menu_overflow_description);
        }
    }

    private final class CompilationItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return displayedItems.size();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
            return new CompilationItemHolder(new PageItemView<Compilation>(getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int pos) {
            ((CompilationItemHolder) holder).bindItem(displayedItems.get(pos));
        }

        @Override public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            ((CompilationItemHolder) holder).getView().setCallback(itemCallback);
        }

        @Override public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            ((CompilationItemHolder) holder).getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ItemCallback implements PageItemView.Callback<Compilation> {
        @Override
        public void onClick(@Nullable Compilation item) {
            if (item != null) {
                startActivity(CompilationDetailActivity.newIntent(getContext(), item));
            }
        }

        @Override
        public boolean onLongClick(@Nullable Compilation item) {
            return true;
        }

        @Override
        public void onThumbClick(@Nullable Compilation item) {
            onClick(item);
        }

        @Override
        public void onActionClick(@Nullable Compilation item, @NonNull PageItemView view) {
        }

        @Override
        public void onSecondaryActionClick(@Nullable Compilation item, @NonNull PageItemView view) {
        }
    }

    private class SearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            appBarLayout.setExpanded(false, true);
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            setSearchQuery(s);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            setSearchQuery(null);
        }

        @Override
        protected String getSearchHintString() {
            return getString(R.string.offline_compilations_search_by_name);
        }
    }

    private class CompilationCallback implements CompilationClient.Callback {
        @Override
        public void success(@NonNull List<Compilation> compilations) {
            allItems = compilations;
            updating = false;
            OfflineManager.instance().updateFromRemoteMetadata(compilations);
            setSearchQuery(currentSearchQuery);
        }

        @Override
        public void error(@NonNull Throwable caught) {
            updating = false;
            lastError = caught;
            updateEmptyState();
        }
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }
}
