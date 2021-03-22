package org.wikipedia.page.tabs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.TabFunnel;
import org.wikipedia.main.MainActivity;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.TabCountsView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.util.logging.LogLevel;

import static org.wikipedia.Constants.InvokeSource.TABS_ACTIVITY;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class TabActivity extends BaseActivity {
    private static final String LAUNCHED_FROM_PAGE_ACTIVITY = "launchedFromPageActivity";

    public static final int RESULT_LOAD_FROM_BACKSTACK = 10;
    public static final int RESULT_NEW_TAB = 11;

    private static final int MAX_CACHED_BMP_SIZE = 800;

    @BindView(R.id.tab_switcher) TabSwitcher tabSwitcher;
    @BindView(R.id.tab_toolbar) Toolbar tabToolbar;
    @BindView(R.id.tab_counts_view) TabCountsView tabCountsView;
    private WikipediaApp app;
    private boolean launchedFromPageActivity;
    private TabListener tabListener = new TabListener();
    private TabFunnel funnel = new TabFunnel();
    private boolean cancelled = true;
    private long tabUpdatedTimeMillis;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    @Nullable private static Bitmap FIRST_TAB_BITMAP;

    public static void captureFirstTabBitmap(@NonNull View view) {
        clearFirstTabBitmap();
        Bitmap bmp = null;
        try {
            boolean wasCacheEnabled = view.isDrawingCacheEnabled();
            if (!wasCacheEnabled) {
                view.setDrawingCacheEnabled(true);
                view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
            }
            Bitmap cacheBmp = view.getDrawingCache();
            if (cacheBmp != null) {
                int width;
                int height;
                if (cacheBmp.getWidth() > cacheBmp.getHeight()) {
                    width = MAX_CACHED_BMP_SIZE;
                    height = width * cacheBmp.getHeight() / cacheBmp.getWidth();
                } else {
                    height = MAX_CACHED_BMP_SIZE;
                    width = height * cacheBmp.getWidth() / cacheBmp.getHeight();
                }
                bmp = Bitmap.createScaledBitmap(cacheBmp, width, height, true);
            }
            if (!wasCacheEnabled) {
                view.setDrawingCacheEnabled(false);
            }
        } catch (OutOfMemoryError e) {
            // don't worry about it
        }
        FIRST_TAB_BITMAP = bmp;
    }

    private static void clearFirstTabBitmap() {
        if (FIRST_TAB_BITMAP != null) {
            if (!FIRST_TAB_BITMAP.isRecycled()) {
                FIRST_TAB_BITMAP.recycle();
            }
            FIRST_TAB_BITMAP = null;
        }
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, TabActivity.class);
    }

    public static Intent newIntentFromPageActivity(@NonNull Context context) {
        return new Intent(context, TabActivity.class)
                .putExtra(LAUNCHED_FROM_PAGE_ACTIVITY, true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);
        ButterKnife.bind(this);
        app = WikipediaApp.getInstance();
        funnel.logEnterList(app.getTabCount());
        tabCountsView.updateTabCount(false);
        launchedFromPageActivity = getIntent().hasExtra(LAUNCHED_FROM_PAGE_ACTIVITY);

        FeedbackUtil.setButtonLongPressToast(tabCountsView);

        setStatusBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground));
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground));
        setSupportActionBar(tabToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        tabSwitcher.setPreserveState(false);
        tabSwitcher.setDecorator(new TabSwitcherDecorator() {
            @NonNull
            @Override
            public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, int viewType) {
                if (viewType == 1) {
                    ImageView view = new AppCompatImageView(TabActivity.this);
                    view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    view.setImageBitmap(FIRST_TAB_BITMAP);
                    view.setPadding(0, topTabLeadImageEnabled() ? 0 : -DimenUtil.getToolbarHeightPx(TabActivity.this), 0, 0);

                    return view;
                }
                return inflater.inflate(R.layout.item_tab_contents, parent, false);
            }

            @Override
            public void onShowTab(@NonNull Context context, @NonNull TabSwitcher tabSwitcher, @NonNull View view,
                                  @NonNull Tab tab, int index, int viewType, @Nullable Bundle savedInstanceState) {
                int tabIndex = app.getTabCount() - index - 1;
                if (viewType == 1 || tabIndex < 0 || app.getTabList().get(tabIndex) == null) {
                    return;
                }
                TextView titleText = view.findViewById(R.id.tab_article_title);
                TextView descriptionText = view.findViewById(R.id.tab_article_description);

                PageTitle title = app.getTabList().get(tabIndex).getBackStackPositionTitle();
                titleText.setText(StringUtil.fromHtml(title.getDisplayText()));

                if (TextUtils.isEmpty(title.getDescription())) {
                    descriptionText.setVisibility(View.GONE);
                } else {
                    descriptionText.setText(title.getDescription());
                    descriptionText.setVisibility(View.VISIBLE);
                }

                setConditionalLayoutDirection(view, title.getWikiSite().languageCode());
            }

            @Override
            public int getViewType(@NonNull final Tab tab, final int index) {
                if (index == 0 && FIRST_TAB_BITMAP != null && !FIRST_TAB_BITMAP.isRecycled()) {
                    return 1;
                }
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }
        });

        for (int i = 0; i < app.getTabList().size(); i++) {
            int tabIndex = app.getTabList().size() - i - 1;
            if (app.getTabList().get(tabIndex).getBackStack().isEmpty()) {
                continue;
            }
            Tab tab = new Tab(StringUtil.fromHtml(app.getTabList().get(tabIndex).getBackStackPositionTitle().getDisplayText()));
            tab.setIcon(R.drawable.ic_image_black_24dp);
            tab.setIconTint(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color));
            tab.setTitleTextColor(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color));
            tab.setCloseButtonIcon(R.drawable.ic_close_white_24dp);
            tab.setCloseButtonIconTint(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color));
            tab.setCloseable(true);
            tab.setParameters(new Bundle());
            tabSwitcher.addTab(tab);
        }

        tabSwitcher.setLogLevel(LogLevel.OFF);
        tabSwitcher.addListener(tabListener);
        tabSwitcher.showSwitcher();
    }

    @OnClick(R.id.tab_counts_view) void onItemClick(View view) {
        onBackPressed();
    }

    @Override
    public void onDestroy() {
        if (cancelled) {
            funnel.logCancel(app.getTabCount());
        }
        tabSwitcher.removeListener(tabListener);
        clearFirstTabBitmap();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        app.commitTabState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tabs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.menu_open_a_new_tab:
                openNewTab();
                return true;
            case R.id.menu_close_all_tabs:
                if (app.getTabList().isEmpty()) {
                    return true;
                }
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage(R.string.close_all_tabs_confirm);
                alert.setPositiveButton(R.string.close_all_tabs_confirm_yes, (dialog, which) -> {
                    tabSwitcher.clear();
                    cancelled = false;
                });
                alert.setNegativeButton(R.string.close_all_tabs_confirm_no, null);
                alert.create().show();
                return true;
            case R.id.menu_save_all_tabs:
                if (!app.getTabList().isEmpty()) {
                    saveTabsToList();
                }
                return true;
            case R.id.menu_explore:
                goToMainTab(NavTab.EXPLORE);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveTabsToList() {
        List<org.wikipedia.page.tabs.Tab> tabsList = app.getTabList();
        List<PageTitle> titlesList = new ArrayList<>();
        for (org.wikipedia.page.tabs.Tab tab : tabsList) {
            titlesList.add(tab.getBackStackPositionTitle());
        }
        bottomSheetPresenter.show(getSupportFragmentManager(),
                AddToReadingListDialog.newInstance(titlesList, TABS_ACTIVITY));
    }

    private boolean topTabLeadImageEnabled() {
        if (app.getTabCount() > 0) {
            PageTitle pageTitle = app.getTabList().get(app.getTabCount() - 1).getBackStackPositionTitle();
            return pageTitle != null && (!pageTitle.isMainPage() && !TextUtils.isEmpty(pageTitle.getThumbUrl()));
        }
        return false;
    }

    private void openNewTab() {
        cancelled = false;
        funnel.logCreateNew(app.getTabCount());
        if (launchedFromPageActivity) {
            setResult(RESULT_NEW_TAB);
        } else {
            startActivity(PageActivity.newIntentForNewTab(TabActivity.this));
        }
        finish();
    }

    private void showUndoSnackbar(final Tab tab, final int index, final org.wikipedia.page.tabs.Tab appTab, final int appTabIndex) {
        if (appTab.getBackStackPositionTitle() == null) {
            return;
        }
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this, getString(R.string.tab_item_closed, appTab.getBackStackPositionTitle().getDisplayText()), FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, v -> {
            app.getTabList().add(appTabIndex, appTab);
            tabSwitcher.addTab(tab, index);
        });
        snackbar.show();
    }

    private void showUndoAllSnackbar(@NonNull final Tab[] tabs, @NonNull List<org.wikipedia.page.tabs.Tab> appTabs) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this, getString(R.string.all_tab_items_closed), FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_item_delete_undo, v -> {
            app.getTabList().addAll(appTabs);
            tabSwitcher.addAllTabs(tabs);
            appTabs.clear();
        });
        snackbar.show();
    }

    private class TabListener implements TabSwitcherListener {
        @Override public void onSwitcherShown(@NonNull TabSwitcher tabSwitcher) { }

        @Override public void onSwitcherHidden(@NonNull TabSwitcher tabSwitcher) { }

        @Override
        public void onSelectionChanged(@NonNull TabSwitcher tabSwitcher, int index, @Nullable Tab selectedTab) {
            int tabIndex = app.getTabList().size() - index - 1;
            L.d("Tab selected: " + index);

            if (tabIndex < 0) {
                return;
            }

            if (tabIndex < app.getTabList().size() - 1) {
                org.wikipedia.page.tabs.Tab tab = app.getTabList().remove(tabIndex);
                app.getTabList().add(tab);
            }
            tabCountsView.updateTabCount(false);
            cancelled = false;

            final int tabUpdateDebounceMillis = 250;
            if (System.currentTimeMillis() - tabUpdatedTimeMillis > tabUpdateDebounceMillis) {
                funnel.logSelect(app.getTabCount(), tabIndex);
                if (launchedFromPageActivity) {
                    setResult(RESULT_LOAD_FROM_BACKSTACK);
                } else {
                    startActivity(PageActivity.newIntent(TabActivity.this));
                }
                finish();
            }
        }

        @Override
        public void onTabAdded(@NonNull TabSwitcher tabSwitcher, int index, @NonNull Tab tab, @NonNull Animation animation) {
            tabCountsView.updateTabCount(false);
            tabUpdatedTimeMillis = System.currentTimeMillis();
        }

        @Override
        public void onTabRemoved(@NonNull TabSwitcher tabSwitcher, int index, @NonNull Tab tab, @NonNull Animation animation) {
            int tabIndex = app.getTabList().size() - index - 1;
            if (tabIndex < 0) {
                return;
            }
            org.wikipedia.page.tabs.Tab appTab = app.getTabList().remove(tabIndex);

            funnel.logClose(app.getTabCount(), tabIndex);
            tabCountsView.updateTabCount(false);
            setResult(RESULT_LOAD_FROM_BACKSTACK);
            showUndoSnackbar(tab, index, appTab, tabIndex);
            tabUpdatedTimeMillis = System.currentTimeMillis();
        }

        @Override
        public void onAllTabsRemoved(@NonNull TabSwitcher tabSwitcher, @NonNull Tab[] tabs, @NonNull Animation animation) {
            L.d("All tabs removed.");
            List<org.wikipedia.page.tabs.Tab> appTabs = new ArrayList<>(app.getTabList());

            app.getTabList().clear();
            tabCountsView.updateTabCount(false);
            setResult(RESULT_LOAD_FROM_BACKSTACK);
            showUndoAllSnackbar(tabs, appTabs);
            tabUpdatedTimeMillis = System.currentTimeMillis();
        }
    }

    private void goToMainTab(NavTab tab) {
        startActivity(MainActivity.newIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
                .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, tab.code()));
        finish();
    }
}
