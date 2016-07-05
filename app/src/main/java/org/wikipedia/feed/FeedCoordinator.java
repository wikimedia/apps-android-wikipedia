package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.continuereading.ContinueReadingClient;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.feed.random.RandomClient;
import org.wikipedia.feed.searchbar.SearchClient;

public class FeedCoordinator extends FeedCoordinatorBase {

    public FeedCoordinator(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void buildScript(int age) {

        // hard-coded list of card types to load when continuing the feed
        if (age == 0) {
            init();
        }

        addPendingClient(new BecauseYouReadClient());
        addPendingClient(new ContinueReadingClient());
        addPendingClient(new AggregatedFeedContentClient());
        if (age == 0) {
            addPendingClient(new RandomClient());
            addPendingClient(new MainPageClient());
        }
    }

    private void init() {
        addPendingClient(new SearchClient());
    }
}
