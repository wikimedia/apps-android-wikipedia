package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.Rule;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.history.HistoryEntry;

@LargeTest
@RunWith(AndroidJUnit4.class)
public abstract class BasePageLoadTest {
    private static final Site DEFAULT_SITE = Site.forLanguage("test");

    @Rule
    @NonNull
    public final ActivityTestRule<PageActivity> activityRule = new ActivityTestRule<>(PageActivity.class);

    protected void loadPage(String title) {
        loadPage(title, new PageLoadLatchCallback());
    }

    protected void loadPage(String title, @NonNull PageLoadLatchCallback callback) {
        getFragment().setPageLoadCallbacks(callback);
        final PageTitle pageTitle = new PageTitle(null, title, getSite());
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getFragment().loadPage(pageTitle,
                        new HistoryEntry(pageTitle, HistoryEntry.SOURCE_RANDOM),
                        PageLoadStrategy.Cache.FALLBACK,
                        false);
            }
        });
        callback.await();
    }

    protected Site getSite() {
        return DEFAULT_SITE;
    }

    protected void runOnMainSync(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    protected PageFragment getFragment() {
        return (PageFragment) getActivity().getTopFragment();
    }

    protected PageActivity getActivity() {
        return activityRule.getActivity();
    }
}
