package org.wikipedia.page;

import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v4.widget.*;
import android.support.v7.app.*;
import com.squareup.otto.*;
import org.wikipedia.*;
import org.wikipedia.events.*;
import org.wikipedia.history.*;
import org.wikipedia.interlanguage.*;
import org.wikipedia.networking.*;
import org.wikipedia.recurring.*;
import org.wikipedia.search.*;

public class PageActivity extends ActionBarActivity {
    public static final String ACTION_PAGE_FOR_TITLE = "org.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";

    private Bus bus;
    private WikipediaApp app;

    private SearchArticlesFragment searchAriclesFragment;
    private DrawerLayout drawerLayout;

    private PageViewFragment curPageFragment;

    private ConnectionChangeReceiver connChangeReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = ((WikipediaApp)getApplicationContext());
        bus = app.getBus();
        bus.register(this);

        searchAriclesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        searchAriclesFragment.setDrawerLayout(drawerLayout);

        if (savedInstanceState == null) {
            // Don't do this if we are just rotating the phone
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
        }

        IntentFilter connFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        connChangeReceiver = new ConnectionChangeReceiver();
        this.registerReceiver(connChangeReceiver, connFilter);
        // Kickstart network ops. currently, just to initiate Wikipedia Zero check
        connChangeReceiver.onReceive(app, getIntent());

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(this).run();
    }

    private void displayNewPage(PageTitle title, HistoryEntry entry) {
        PageViewFragment pageFragment = new PageViewFragment(title, entry, R.id.search_fragment);
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
    public void onOtherLanguagesEvent(ShowOtherLanguagesEvent event) {
        Intent shareIntent = new Intent();
        shareIntent.setClass(this, LangLinksActivity.class);
        shareIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        shareIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, curPageFragment.getTitle());
        startActivity(shareIntent);
    }

    @Subscribe
    public void onShowToCEvent(ShowToCEvent event) {
        curPageFragment.showToC();
    }
    @Override
    public void onBackPressed() {
        if ((curPageFragment != null && !curPageFragment.handleBackPressed())
                && !searchAriclesFragment.handleBackPressed()) {
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (connChangeReceiver != null) {
            this.unregisterReceiver(connChangeReceiver);
        }
    }
}
