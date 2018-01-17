package org.wikipedia.offline;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.OfflineLibraryFunnel;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.SearchEmptyView;
import org.wikipedia.views.WikiErrorView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.wikipedia.util.DateUtil.getShortDateString;
import static org.wikipedia.util.FileUtil.bytesToUserVisibleUnit;

public class LocalCompilationsFragment extends DownloadObserverFragment {
    @BindView(R.id.compilation_list_container) View listContainer;
    @BindView(R.id.compilation_list) RecyclerView recyclerView;
    @BindView(R.id.search_empty_view) SearchEmptyView searchEmptyView;
    @BindView(R.id.compilation_search_progress_bar) ProgressBar progressBar;
    @BindView(R.id.compilations_count_text) TextView countText;
    @BindView(R.id.disk_usage_view) DiskUsageView diskUsageView;
    @BindView(R.id.compilation_search_error) WikiErrorView errorView;
    @BindView(R.id.compilation_empty_container) View emptyContainer;
    @BindView(R.id.compilation_empty_description) TextView emptyDescription;
    @BindView(R.id.compilation_packs_hint) TextView packsHint;
    @BindView(R.id.compilation_data_usage_hint) TextView dataUsageHint;
    private Unbinder unbinder;

    private boolean updating;
    private Throwable lastError;
    private CompilationItemAdapter adapter = new CompilationItemAdapter();
    private ItemCallback itemCallback = new ItemCallback();
    private CompilationClientCallback compilationClientCallback = new CompilationClientCallback();
    private OfflineLibraryFunnel funnel;

    @NonNull private List<Compilation> displayedItems = new ArrayList<>();

    public interface Callback {
        void onRequestUpdateCompilations();
    }

    @NonNull
    public static LocalCompilationsFragment newInstance() {
        return new LocalCompilationsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_local_compilations, container, false);
        unbinder = ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DrawableItemDecoration(getContext(), R.attr.list_separator_drawable));
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        errorView.setBackClickListener(v -> getActivity().finish());

        emptyDescription.setMovementMethod(LinkMovementMethod.getInstance());
        emptyDescription.setText(StringUtil.fromHtml(getString(R.string.offline_library_empty_description_sideload)));
        RichTextUtil.removeUnderlinesFromLinks(emptyDescription);
        packsHint.setMovementMethod(LinkMovementMethod.getInstance());
        packsHint.setText(StringUtil.fromHtml(getString(R.string.offline_library_packs_hint)));
        RichTextUtil.removeUnderlinesFromLinks(packsHint);
        dataUsageHint.setMovementMethod(new LinkMovementMethodExt((url, titleString) -> {
            if (url.equals(UriUtil.LOCAL_URL_SETTINGS)) {
                startActivity(SettingsActivity.newIntent(getContext()));
            }
        }));
        dataUsageHint.setText(StringUtil.fromHtml(getString(R.string.offline_library_data_usage_hint)));
        RichTextUtil.removeUnderlinesFromLinks(dataUsageHint);

        funnel = new OfflineLibraryFunnel(WikipediaApp.getInstance(), 0);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        beginUpdate();
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        funnel.done(OfflineManager.instance().compilations());
        recyclerView.setAdapter(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @OnClick({R.id.compilations_add_button, R.id.compilation_empty_search_button}) void onAddCompilationClick() {
        startActivity(RemoteCompilationsActivity.newIntent(getContext()));
    }

    @Override
    protected void onPollDownloads() {
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    public void onCompilationsRefreshed() {
        updating = false;
        lastError = null;
        update();
        new CompilationClient().request(compilationClientCallback);
    }

    public void onCompilationsError(Throwable t) {
        updating = false;
        lastError = t;
        update();
    }

    private void postBeginUpdate() {
        listContainer.post(this::beginUpdate);
    }

    private void beginUpdate() {
        updating = true;
        lastError = null;
        if (callback() != null) {
            callback().onRequestUpdateCompilations();
        }
        update();
    }

    private void update() {
        displayedItems.clear();
        displayedItems.addAll(OfflineManager.instance().compilations());
        countText.setText(getString(R.string.offline_compilations_found_count, displayedItems.size()));
        adapter.notifyDataSetChanged();
        updateEmptyState();

        long totalBytes = 0;
        for (Compilation c : OfflineManager.instance().compilations()) {
            totalBytes += c.size();
        }
        diskUsageView.update(totalBytes);
    }

    private void updateEmptyState() {
        if (lastError != null) {
            errorView.setError(lastError);
            errorView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            searchEmptyView.setVisibility(View.GONE);
            listContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.GONE);
            return;
        }
        errorView.setVisibility(View.GONE);
        progressBar.setVisibility(updating ? View.VISIBLE : View.GONE);
        searchEmptyView.setVisibility(View.GONE);
        listContainer.setVisibility(displayedItems.isEmpty() ? View.GONE : View.VISIBLE);
        emptyContainer.setVisibility(displayedItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class CompilationItemHolder extends DefaultViewHolder<PageItemView<Compilation>> {
        private Compilation compilation;
        private CompilationDownloadControlView controlView;
        private boolean wasDownloading;

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
            if (myItem == null && wasDownloading) {
                postBeginUpdate();
                wasDownloading = false;
            } else if (myItem != null) {
                wasDownloading = true;
            }
            if (compilation == this.compilation) {
                return;
            }
            wasDownloading = false;
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
            return new CompilationItemHolder(new PageItemView<>(getContext()));
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
        public void onClick(@Nullable Compilation comp) {
            if (comp != null) {
                startActivity(CompilationDetailActivity.newIntent(getContext(), comp));
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

    private void showCompilationOverflowMenu(@NonNull final Compilation compilation, @NonNull View anchorView) {
        PopupMenu menu = new PopupMenu(anchorView.getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_local_compilation_item, menu.getMenu());
        menu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_compilation_share:
                    share(compilation);
                    return false;
                case R.id.menu_compilation_remove:
                    remove(compilation);
                    return false;
                default:
                    return false;
            }
        });
        menu.show();
    }

    private void share(@NonNull Compilation compilation) {
        Intent intent = ShareCompat.IntentBuilder.from(getActivity())
                .setType("*/*")
                .setStream(ShareUtil.getUriFromFile(getContext(), new File(compilation.path())))
                .getIntent();
        startActivity(ShareUtil.createChooserIntent(intent, getString(R.string.share_via), getContext()));
        funnel.share();
    }

    private void remove(@NonNull final Compilation compilation) {
        getDownloadObserver().removeWithConfirmation(getActivity(), compilation, (dialog, which) -> beginUpdate());
    }

    private class CompilationClientCallback implements CompilationClient.Callback {
        @Override
        public void success(@NonNull List<Compilation> compilations) {
            if (!isAdded()) {
                return;
            }
            OfflineManager.instance().updateFromRemoteMetadata(compilations);
            update();
        }

        @Override
        public void error(@NonNull Throwable caught) {
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
