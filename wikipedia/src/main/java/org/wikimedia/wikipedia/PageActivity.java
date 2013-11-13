package org.wikimedia.wikipedia;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import org.wikimedia.wikipedia.events.NewWikiPageNavigationEvent;

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

    @Subscribe
    public void onNewWikiPageNavigationEvent(NewWikiPageNavigationEvent event) {
        displayNewPage(event.getTitle());
    }

    @Override
    public void onBackPressed() {
        if (!searchAriclesFragment.handleBackPressed()) {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    protected void onStart() {
        super.onResume();
        bus = app.getBus();
        bus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bus.unregister(this);
    }
}
