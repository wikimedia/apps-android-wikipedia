package org.wikimedia.wikipedia;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;
import org.wikimedia.wikipedia.history.HistoryEntry;
import org.wikimedia.wikipedia.history.HistoryEntryPersister;

public class PageActivity extends FragmentActivity {
    private Bus bus;
    private WikipediaApp app;

    private SearchArticlesFragment searchAriclesFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = ((WikipediaApp)getApplicationContext());
        searchAriclesFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
    }

    private void displayNewPage(PageTitle title) {
        PageViewFragment pageFragment = new PageViewFragment(title);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.content_frame, pageFragment)
                .addToBackStack(null)
                .commit();
    }

    private HistoryEntryPersister historyEntryPersister;
    @Subscribe
    public void onNewWikiPageNavigationEvent(NewWikiPageNavigationEvent event) {
        displayNewPage(event.getTitle());
        if (historyEntryPersister == null) {
            historyEntryPersister = new HistoryEntryPersister(this);
        }
        historyEntryPersister.persist(event.getHistoryEntry());
    }

    @Override
    public void onBackPressed() {
        if (!searchAriclesFragment.handleBackPressed()) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
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
        bus = app.getBus();
        bus.register(this);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Site site = new Site(intent.getData().getAuthority());
            PageTitle title = site.titleForInternalLink(intent.getData().getPath());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);
            bus.post(new NewWikiPageNavigationEvent(title, historyEntry));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
        if (historyEntryPersister != null) {
            historyEntryPersister.cleanup();
            historyEntryPersister = null;
        }
    }
}
