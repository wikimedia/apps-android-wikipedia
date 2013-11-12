package org.wikimedia.wikipedia;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.*;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class PageActivity extends FragmentActivity {
    private Bus bus;
    private WikipediaApp app;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        app = ((WikipediaApp)getApplicationContext());
        bus = app.getBus();
        bus.register(this);
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
    public void onNewWikiPageNavigationEvent(LinkHandler.NewWikiPageNavigationEvent event) {
        displayNewPage(event.getTitle());
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().popBackStack();
    }
}
