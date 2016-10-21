package org.wikipedia.page;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;

import com.squareup.spoon.Spoon;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.tabs.TabsProvider;

import static org.wikipedia.test.TestUtil.runOnMainSync;

@LargeTest
@RunWith(AndroidJUnit4.class)
public abstract class BasePageLoadTest {
    protected static final WikiSite TEST_WIKI = WikiSite.forLanguageCode("test");
    protected static final WikiSite EN_WIKI = WikiSite.forLanguageCode("en");

    @Rule
    @NonNull
    public final ActivityTestRule<PageActivity> activityRule = new ActivityTestRule<>(PageActivity.class);

    protected void loadPageSync(String title) {
        loadPage(title);
    }

    protected void loadPageSync(String title, @NonNull PageLoadLatchCallback callback) {
        loadPageSync(title, TEST_WIKI, callback);
    }

    protected void loadPageSync(String title, @NonNull WikiSite wiki) {
        loadPageSync(title, wiki, new PageLoadLatchCallback());
    }

    protected void loadPageSync(String title, @NonNull WikiSite wiki,
                                @NonNull PageLoadLatchCallback callback) {
        getActivity().setPageLoadCallbacks(callback);
        loadPage(title, wiki);
        callback.await();
    }

    protected void loadPage(String title) {
        loadPage(title, TEST_WIKI);
    }

    protected void loadPage(String title, @NonNull WikiSite wiki) {
        final PageTitle pageTitle = new PageTitle(null, title, wiki);
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().loadPage(pageTitle, new HistoryEntry(pageTitle,
                        HistoryEntry.SOURCE_RANDOM),
                        TabsProvider.TabPosition.CURRENT_TAB,
                        false);
            }
        });
    }

    protected void requestLandscapeOrientation() {
        requestOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    protected void requestPortraitOrientation() {
        requestOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    protected void screenshot(String tag) {
        if (screenshotPermitted()) {
            Spoon.screenshot(getActivity(), tag);
        }
    }

    private boolean screenshotPermitted() {
        return ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    protected PageActivity getActivity() {
        return activityRule.getActivity();
    }

    private void requestOrientation(int orientation) {
        getActivity().setRequestedOrientation(orientation);
    }
}