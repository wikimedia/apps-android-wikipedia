package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.Site;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.continuereading.ContinueReadingClient;
import org.wikipedia.feed.demo.IntegerListClient;
import org.wikipedia.feed.searchbar.SearchClient;

public class FeedCoordinator extends FeedCoordinatorBase {

    public FeedCoordinator(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void buildScript(Site site, int age) {

        // hard-coded list of card types to load when continuing the feed

        if (age == 0) {
            init();
        }

        addPendingClient(new BecauseYouReadClient(site));
        addPendingClient(new ContinueReadingClient());
        addPendingClient(new IntegerListClient());

    }

    private void init() {
        addPendingClient(new SearchClient());
    }
}
