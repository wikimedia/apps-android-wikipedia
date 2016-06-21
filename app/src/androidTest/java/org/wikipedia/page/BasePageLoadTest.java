package org.wikipedia.page;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.ActivityCompat;
import android.test.suitebuilder.annotation.LargeTest;

import com.squareup.spoon.Spoon;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.MainActivity;
import org.wikipedia.history.HistoryEntry;

@LargeTest
@RunWith(AndroidJUnit4.class)
public abstract class BasePageLoadTest {
    protected static final Site TEST_SITE = Site.forLanguageCode("test");
    protected static final Site EN_SITE = Site.forLanguageCode("en");

    @Rule
    @NonNull
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    protected void loadPageSync(String title) {
        loadPage(title, TEST_SITE);
    }

    protected void loadPageSync(String title, @NonNull PageLoadLatchCallback callback) {
        loadPageSync(title, TEST_SITE, callback);
    }

    protected void loadPageSync(String title, @NonNull Site site) {
        loadPageSync(title, site, new PageLoadLatchCallback());
    }

    protected void loadPageSync(String title,
                                @NonNull Site site,
                                @NonNull PageLoadLatchCallback callback) {
        getFragment().setPageLoadCallbacks(callback);
        loadPage(title, site);
        callback.await();
    }

    protected void loadPage(String title) {
        loadPage(title, TEST_SITE);
    }

    protected void loadPage(String title, @NonNull Site site) {
        final PageTitle pageTitle = new PageTitle(null, title, site);
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getFragment().loadPage(pageTitle,
                        new HistoryEntry(pageTitle, HistoryEntry.SOURCE_RANDOM),
                        PageLoadStrategy.Cache.FALLBACK,
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

    protected void runOnMainSync(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    protected PageFragment getFragment() {
        return (PageFragment) getActivity().getTopFragment();
    }

    protected MainActivity getActivity() {
        return activityRule.getActivity();
    }

    private void requestOrientation(int orientation) {
        getActivity().setRequestedOrientation(orientation);
    }
}