package org.wikimedia.wikipedia;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikimedia.wikipedia.events.PageStateChangeEvent;
import org.wikimedia.wikipedia.events.SavePageEvent;
import org.wikimedia.wikipedia.events.SharePageEvent;
import org.wikimedia.wikipedia.history.HistoryEntry;
import org.wikimedia.wikipedia.history.HistoryEntryPersister;
import org.wikimedia.wikipedia.recurring.RecurringTasksExecutor;
import org.wikimedia.wikipedia.savedpages.SavePageTask;

public class PageActivity extends FragmentActivity {
    public static final String ACTION_PAGE_FOR_TITLE = "org.wikimedia.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikimedia.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikimedia.wikipedia.history.historyentry";

    private Bus bus;
    private WikipediaApp app;

    private SearchArticlesFragment searchAriclesFragment;
    private DrawerLayout drawerLayout;

    private PageViewFragment curPageFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = ((WikipediaApp)getApplicationContext());
        searchAriclesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        bus = app.getBus();
        bus.register(this);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Site site = new Site(intent.getData().getAuthority());
            PageTitle title = site.titleForInternalLink(intent.getData().getPath());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);
            bus.post(new NewWikiPageNavigationEvent(title, historyEntry));
        } else if (ACTION_PAGE_FOR_TITLE.equals(intent.getAction())) {
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
            bus.post(new NewWikiPageNavigationEvent(title, historyEntry));
        }

        searchAriclesFragment.setDrawerLayout(drawerLayout);

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(this).run();
    }

    private void displayNewPage(PageTitle title, HistoryEntry entry) {
        PageViewFragment pageFragment = new PageViewFragment(title, entry);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content_frame, pageFragment)
                .addToBackStack(null)
                .commit();
        this.curPageFragment = pageFragment;
    }

    @Subscribe
    public void onNewWikiPageNavigationEvent(NewWikiPageNavigationEvent event) {
        displayNewPage(event.getTitle(), event.getHistoryEntry());
    }

    @Subscribe
    public void onPageSaveEvent(SavePageEvent event) {
        curPageFragment.savePage();
    }

    @Subscribe
    public void onSharePageEvent(SharePageEvent event) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, curPageFragment.getTitle().getDisplayText() + " " + curPageFragment.getTitle().getCanonicalUri());
        shareIntent.setType("text/plain");
        startActivity(shareIntent);
    }

    @Subscribe
    public void onPageStateChange(PageStateChangeEvent event) {
        if (event.getState() == PageViewFragment.STATE_COMPLETE_FETCH) {
            // This could potentially be called *before* onCreateView of the PageViewFragment is done
            // And for *some* reason, that causes all the internal variables to be null
            // That is super weird and makes no sense to me - I verified they are the same objects
            // Using the debugger. But expanding them has all member variables that were not set in the
            // constructor to be null.
            // So this 'hack' works around it by delaying the setup by 100ms.
            // FIXME: Find out if this is a WTF Java or a WTF Android
            new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    new QuickReturnHandler(curPageFragment.getObservableWebView(), searchAriclesFragment.getView());
                    return true;
                }
            }).sendEmptyMessageDelayed(0, 100);
        }
    }

    @Override
    public void onBackPressed() {
        if (!searchAriclesFragment.handleBackPressed()) {
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                // Everything we could pop has been popped....
                finish();
            } else {
                getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bus == null) {
            bus = app.getBus();
            bus.register(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
        bus = null;
    }
}
