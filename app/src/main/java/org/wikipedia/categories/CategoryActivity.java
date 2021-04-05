package org.wikipedia.categories;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;

public class CategoryActivity extends BaseActivity implements LinkPreviewDialog.Callback {
    private static final String EXTRA_TITLE = "categoryTitle";

    @BindView(R.id.category_error) WikiErrorView errorView;
    @BindView(R.id.category_progress) ProgressBar progressBar;
    @BindView(R.id.category_recycler) RecyclerView categoryRecycler;
    @BindView(R.id.category_tab_layout) TabLayout tabLayout;

    private PageTitle categoryTitle;
    private List<PageTitle> unsortedTitleList = new ArrayList<>();
    private List<PageTitle> unsortedSubcategoryList = new ArrayList<>();
    private List<PageTitle> titleList = new ArrayList<>();
    private ItemCallback itemCallback = new ItemCallback();
    private boolean showSubcategories;
    private List<PageTitle> pendingItemsForHydration = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();
    private Runnable hydrationRunnable = this::hydrateTitles;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    public static Intent newIntent(@NonNull Context context, @NonNull PageTitle categoryTitle) {
        return new Intent(context, CategoryActivity.class)
                .putExtra(EXTRA_TITLE, categoryTitle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        ButterKnife.bind(this);

        setStatusBarColor(ResourceUtil.getThemedColor(this, android.R.attr.windowBackground));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        categoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        categoryRecycler.setAdapter(new CategoryAdapter());

        categoryTitle = getIntent().getParcelableExtra(EXTRA_TITLE);
        getSupportActionBar().setTitle(categoryTitle.getDisplayText());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showSubcategories = tab.getPosition() == 1;
                layOutTitles();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        loadCategory();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_tabs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void loadPage(@NonNull PageTitle title) {
        if (title.namespace() == Namespace.CATEGORY) {
            startActivity(newIntent(this, title));
        } else {
            HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_CATEGORY);
            bottomSheetPresenter.show(getSupportFragmentManager(), LinkPreviewDialog.newInstance(entry, null));
        }
    }

    private void loadCategory() {
        disposables.clear();
        errorView.setVisibility(View.GONE);
        categoryRecycler.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        disposables.add(ServiceFactory.get(categoryTitle.getWikiSite()).getCategoryMembers(categoryTitle.getPrefixedText(), null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> progressBar.setVisibility(View.GONE))
                .subscribe(response -> {
                    unsortedTitleList.clear();
                    for (MwQueryPage page : response.query().categoryMembers()) {
                        PageTitle title = new PageTitle(page.title(), categoryTitle.getWikiSite());
                        if (page.namespace() == Namespace.CATEGORY) {
                            unsortedSubcategoryList.add(title);
                        } else {
                            unsortedTitleList.add(title);
                        }
                    }
                    layOutTitles();
                }, throwable -> {
                    errorView.setError(throwable);
                    errorView.setVisibility(View.VISIBLE);
                    L.e(throwable);
                }));
    }

    private void layOutTitles() {
        titleList.clear();
        titleList.addAll(showSubcategories ? unsortedSubcategoryList : unsortedTitleList);

        if (titleList.isEmpty()) {
            categoryRecycler.setVisibility(View.GONE);
        }
        categoryRecycler.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
        categoryRecycler.getAdapter().notifyDataSetChanged();
    }

    private void queueForHydration(PageTitle title) {
        final int maxQueueSize = 50;
        final int runnableDelay = 500;
        if (title.getDescription() != null || title.namespace() != Namespace.MAIN) {
            return;
        }
        pendingItemsForHydration.add(title);
        categoryRecycler.removeCallbacks(hydrationRunnable);
        if (pendingItemsForHydration.size() >= maxQueueSize) {
            hydrateTitles();
        } else {
            categoryRecycler.postDelayed(hydrationRunnable, runnableDelay);
        }
    }

    private void hydrateTitles() {
        List<PageTitle> titles = new ArrayList<>(pendingItemsForHydration);
        pendingItemsForHydration.clear();
        disposables.add(ServiceFactory.get(categoryTitle.getWikiSite()).getImagesAndThumbnails(StringUtils.join(titles, '|'))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    for (MwQueryPage page : response.query().pages()) {
                        for (PageTitle title : titles) {
                            if (title.getDisplayText().equals(page.title())) {
                                title.setThumbUrl(page.thumbUrl());
                                title.setDescription(TextUtils.isEmpty(page.description()) ? "" : page.description());
                                break;
                            }
                        }
                    }
                    categoryRecycler.getAdapter().notifyDataSetChanged();
                }, L::e));
    }

    @Override
    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        startActivity(inNewTab ? PageActivity.newIntentForNewTab(this, entry, entry.getTitle())
                : PageActivity.newIntentForCurrentTab(this, entry, entry.getTitle()));
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        ClipboardUtil.setPlainText(this, null, title.getUri());
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        bottomSheetPresenter.showAddToListDialog(getSupportFragmentManager(), title, LINK_PREVIEW_MENU);
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    private class CategoryItemHolder extends RecyclerView.ViewHolder {
        private PageItemView itemView;

        CategoryItemHolder(PageItemView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        void bindItem(PageTitle title) {
            itemView.setItem(title);
            itemView.setTitle(title.namespace() != Namespace.CATEGORY
                    ? title.getDisplayText() : title.getText().replace("_", " "));
            itemView.setImageUrl(title.getThumbUrl());
            itemView.setDescription(title.getDescription());
        }

        public PageItemView getView() {
            return itemView;
        }
    }

    private final class CategoryAdapter extends RecyclerView.Adapter<CategoryItemHolder> {
        @Override
        public int getItemCount() {
            return titleList.size();
        }

        @Override
        public CategoryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            PageItemView<PageTitle> view = new PageItemView<>(CategoryActivity.this);
            return new CategoryItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryItemHolder holder, int pos) {
            holder.bindItem(titleList.get(pos));
            queueForHydration(titleList.get(pos));
        }

        @Override public void onViewAttachedToWindow(@NonNull CategoryItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(itemCallback);
        }

        @Override public void onViewDetachedFromWindow(@NonNull CategoryItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ItemCallback implements PageItemView.Callback<PageTitle> {

        @Override
        public void onClick(@Nullable PageTitle item) {
            if (item != null) {
                loadPage(item);
            }
        }

        @Override
        public boolean onLongClick(@Nullable PageTitle item) {
            return false;
        }

        @Override
        public void onThumbClick(@Nullable PageTitle item) {
            if (item != null) {
                loadPage(item);
            }
        }

        @Override
        public void onActionClick(@Nullable PageTitle item, @NonNull View view) {
        }

        @Override
        public void onListChipClick(@Nullable ReadingList readingList) {
        }
    }
}
