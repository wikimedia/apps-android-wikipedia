package org.wikipedia.offline;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class LocalCompilationsFragment extends Fragment {
    @BindView(R.id.compilation_list_container) View listContainer;
    @BindView(R.id.compilation_list) RecyclerView recyclerView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.compilation_search_progress_bar) ProgressBar progressBar;
    @BindView(R.id.compilations_count_text) TextView countText;
    @BindView(R.id.disk_usage_view) DiskUsageView diskUsageView;
    private Unbinder unbinder;

    private boolean updating;
    private CompilationItemAdapter adapter = new CompilationItemAdapter();
    private ItemCallback itemCallback = new ItemCallback();

    private SearchCallback searchActionModeCallback = new SearchCallback();
    @NonNull private List<Compilation> displayedItems = new ArrayList<>();
    private String currentSearchQuery;

    public interface Callback {
        void onRequestUpdateCompilations();
    }

    @NonNull
    public static LocalCompilationsFragment newInstance() {
        LocalCompilationsFragment instance = new LocalCompilationsFragment();
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_local_compilations, container, false);
        unbinder = ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));

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
        recyclerView.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_local_compilations, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
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

    public void onCompilationsRefreshed() {
        updating = false;
        update();
    }

    public void onCompilationsError(Throwable t) {
        updating = false;
        // TODO: show error
    }

    private void beginUpdate() {
        updating = true;
        if (callback() != null) {
            callback().onRequestUpdateCompilations();
        }
        updateEmptyState(currentSearchQuery);
    }

    private void update() {
        setSearchQuery(currentSearchQuery);
        long totalBytes = 0;
        for (Compilation c : OfflineManager.instance().compilations()) {
            totalBytes += c.size();
        }
        diskUsageView.update(totalBytes);
    }

    private void setSearchQuery(@Nullable String query) {
        currentSearchQuery = query;
        displayedItems.clear();
        if (TextUtils.isEmpty(query)) {
            displayedItems.addAll(OfflineManager.instance().compilations());
        } else {
            query = query.toUpperCase();
            for (Compilation c : OfflineManager.instance().compilations()) {
                if (c.name().toUpperCase().contains(query.toUpperCase())) {
                    displayedItems.add(c);
                }
            }
        }
        countText.setText(getString(R.string.offline_compilations_found_count, displayedItems.size()));
        adapter.notifyDataSetChanged();
        updateEmptyState(query);
    }

    private void updateEmptyState(@Nullable String searchQuery) {
        if (updating) {
            progressBar.setVisibility(View.VISIBLE);
            searchEmptyView.setVisibility(View.GONE);
            listContainer.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            if (TextUtils.isEmpty(searchQuery)) {
                searchEmptyView.setVisibility(View.GONE);
                listContainer.setVisibility(View.VISIBLE);
            } else {
                listContainer.setVisibility(displayedItems.isEmpty() ? View.GONE : View.VISIBLE);
                searchEmptyView.setVisibility(displayedItems.isEmpty() ? View.VISIBLE : View.GONE);
            }
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
            //TODO: getView().setImageUrl( ... );
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

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
