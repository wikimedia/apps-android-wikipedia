package org.wikipedia.main;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.appshortcuts.AppShortcuts;
import org.wikipedia.database.room.History;
import org.wikipedia.database.room.WikipediaRoomDatabase;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.onboarding.InitialOnboardingActivity;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.tabs.TabActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.ImageZoomHelper;
import org.wikipedia.views.TabCountsView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_INITIAL_ONBOARDING;

public class MainActivity extends SingleFragmentActivity<MainFragment> implements MainFragment.Callback{

    @BindView(R.id.single_fragment_toolbar) Toolbar toolbar;
    @BindView(R.id.single_fragment_toolbar_wordmark) ImageView wordMark;
    private ImageZoomHelper imageZoomHelper;

    private boolean controlNavTabInFragment;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);
        AppShortcuts.setShortcuts(this);
        imageZoomHelper = new ImageZoomHelper(this);

        if (Prefs.isInitialOnboardingEnabled() && savedInstanceState == null) {
            // Updating preference so the search multilingual tooltip
            // is not shown again for first time users
            Prefs.setMultilingualSearchTutorialEnabled(false);

            // Use startActivityForResult to avoid preload the Feed contents before finishing the initial onboarding.
            // The ACTIVITY_REQUEST_INITIAL_ONBOARDING has not been used in any onActivityResult
            startActivityForResult(InitialOnboardingActivity.newIntent(this), ACTIVITY_REQUEST_INITIAL_ONBOARDING);
        }

        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.nav_tab_background_color));
        setSupportActionBar(getToolbar());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        getToolbar().setNavigationIcon(null);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                WikipediaRoomDatabase.Companion.getAppRoomDatabase(MainActivity.this).historyDAO().insertUsers(new History(1,"site","title"));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getFragment().requestUpdateToolbarElevation();
        MenuItem tabsItem = menu.findItem(R.id.menu_tabs);
        if (WikipediaApp.getInstance().getTabCount() < 1 || (getFragment().getCurrentFragment() instanceof SuggestedEditsTasksFragment)) {
            tabsItem.setVisible(false);
        } else {
            tabsItem.setVisible(true);
            TabCountsView tabCountsView = new TabCountsView(this, null);
            tabCountsView.setOnClickListener(v -> {
                if (WikipediaApp.getInstance().getTabCount() == 1) {
                    startActivity(PageActivity.newIntent(MainActivity.this));
                } else {
                    startActivityForResult(TabActivity.newIntent(MainActivity.this), Constants.ACTIVITY_REQUEST_BROWSE_TABS);
                }
            });
            tabCountsView.updateTabCount();
            tabCountsView.setContentDescription(getString(R.string.menu_page_show_tabs));
            tabsItem.setActionView(tabCountsView);
            tabsItem.expandActionView();
            FeedbackUtil.setToolbarButtonLongPressToast(tabCountsView);
        }
        return true;
    }

    @LayoutRes
    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override protected MainFragment createFragment() {
        return MainFragment.newInstance();
    }

    @Override
    public void onTabChanged(@NonNull NavTab tab) {
        if (tab.equals(NavTab.EXPLORE)) {
            wordMark.setVisibility(VISIBLE);
            toolbar.setTitle("");
            controlNavTabInFragment = false;
        } else {
            if (tab.equals(NavTab.HISTORY) && getFragment().getCurrentFragment() != null) {
                ((HistoryFragment) getFragment().getCurrentFragment()).refresh();
            }

            if (tab.equals(NavTab.SUGGESTED_EDITS)) {
                getFragment().hideNavTabOverlayLayout();
            }

            wordMark.setVisibility(GONE);
            toolbar.setTitle(tab.text());
            controlNavTabInFragment = true;
        }
        getFragment().requestUpdateToolbarElevation();
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        if (!controlNavTabInFragment) {
            getFragment().setBottomNavVisible(false);
        }
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        getFragment().setBottomNavVisible(true);
    }

    @Override
    public void updateToolbarElevation(boolean elevate) {
        if (elevate) {
            setToolbarElevationDefault();
        } else {
            clearToolbarElevation();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        getFragment().handleIntent(intent);
    }

    @Override
    protected void onGoOffline() {
        getFragment().onGoOffline();
    }

    @Override
    protected void onGoOnline() {
        getFragment().onGoOnline();
    }

    @Override
    public void onBackPressed() {
        if (getFragment().onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return imageZoomHelper.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public boolean isCurrentFragmentSelected(@NonNull Fragment fragment) {
        return getFragment().getCurrentFragment() == fragment;
    }

    protected void setToolbarElevationDefault() {
        getToolbar().setElevation(DimenUtil.dpToPx(DimenUtil.getDimension(R.dimen.toolbar_default_elevation)));
    }

    protected void clearToolbarElevation() {
        getToolbar().setElevation(0f);
    }
}
