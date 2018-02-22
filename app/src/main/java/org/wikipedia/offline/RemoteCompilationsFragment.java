package org.wikipedia.offline;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
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
import org.wikipedia.gallery.MediaDownloadReceiver;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.util.DateUtil.getShortDateString;
import static org.wikipedia.util.FileUtil.bytesToUserVisibleUnit;

public class RemoteCompilationsFragment extends DownloadObserverFragment {
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
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(), R.attr.list_separator_drawable));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        errorView.setRetryClickListener((v) -> beginUpdate());

        errorView.setBackClickListener((v) -> getActivity().finish());

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
        new CompilationClient().request(compilationCallback);
        updateEmptyState();
    }

    private void setSearchQuery(@Nullable String query) {
        currentSearchQuery = query;
        displayedItems.clear();
        if (TextUtils.isEmpty(query)) {
            displayedItems.addAll(allItems);
        } else {
            query = query.toUpperCase(Locale.getDefault());
            for (Compilation c : allItems) {
                if (c.name().toUpperCase(Locale.getDefault())
                        .contains(query.toUpperCase(Locale.getDefault()))) {
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

    @Override
    protected void onPollDownloads() {
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    private class CompilationItemHolder extends DefaultViewHolder<PageItemView<Compilation>> {
        private Compilation compilation;
        private CompilationDownloadControlView controlView;

        CompilationItemHolder(PageItemView<Compilation> itemView) {
            super(itemView);
            controlView = new CompilationDownloadControlView(itemView.getContext());
            itemView.addFooter(controlView);
            controlView.setPadding(DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.activity_horizontal_margin)),
                    DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.list_item_footer_padding)),
                    DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.activity_horizontal_margin)),
                    DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.list_item_footer_padding)));
            controlView.setBackgroundColor(ResourceUtil.getThemedColor(getContext(), android.R.attr.colorBackground));
            controlView.setCallback(() -> getDownloadObserver().remove(compilation));
        }

        void bindItem(Compilation compilation) {
            DownloadManagerItem myItem = null;
            for (DownloadManagerItem item : getCurrentDownloads()) {
                if (item.is(compilation)) {
                    myItem = item;
                    break;
                }
            }
            if (CompilationDownloadControlView.shouldShowControls(myItem)) {
                controlView.setVisibility(View.VISIBLE);
                controlView.update(myItem);
            } else {
                controlView.setVisibility(View.GONE);
            }
            if (compilation == this.compilation) {
                return;
            }
            this.compilation = compilation;
            getView().setItem(compilation);
            getView().setTitle(compilation.name());
            getView().setDescription(getString(R.string.offline_compilation_detail_date_size_v2,
                    getShortDateString(compilation.date()), bytesToUserVisibleUnit(getContext(), compilation.size())));
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
        public void onActionClick(@Nullable Compilation item, @NonNull View view) {
            if (item != null) {
                showCompilationOverflowMenu(item, view);
            }
        }

        @Override
        public void onSecondaryActionClick(@Nullable Compilation item, @NonNull View view) {
        }
    }

    private class SearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            recyclerView.stopScroll();
            appBarLayout.setExpanded(false, false);
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
            if (!isAdded()) {
                return;
            }
            OfflineManager.instance().updateFromRemoteMetadata(compilations);

            allItems.clear();
            for (Compilation remote : compilations) {
                boolean haveLocal = false;
                for (Compilation local : OfflineManager.instance().compilations()) {
                    if (local.pathNameMatchesUri(remote.uri())) {
                        haveLocal = true;
                        allItems.add(local);
                    }
                }
                if (!haveLocal) {
                    allItems.add(remote);
                }
            }

            updating = false;
            setSearchQuery(currentSearchQuery);
        }

        @Override
        public void error(@NonNull Throwable caught) {
            if (!isAdded()) {
                return;
            }
            updating = false;
            lastError = caught;
            updateEmptyState();
        }
    }

    private void showCompilationOverflowMenu(@NonNull final Compilation compilation, @NonNull View anchorView) {
        PopupMenu menu = new PopupMenu(anchorView.getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_remote_compilation_item, menu.getMenu());
        menu.setOnMenuItemClickListener((menuItem) -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_compilation_download:
                    if (DeviceUtil.isOnline()) {
                        if (!getDownloadObserver().isDownloading(compilation)) {
                            MediaDownloadReceiver.download(getContext(), compilation);
                        }
                    } else {
                        FeedbackUtil.showMessage(getActivity(), R.string.offline_compilation_download_device_offline);
                    }
                    return false;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }
}
