package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.announcement.AnnouncementClient;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.continuereading.ContinueReadingClient;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.feed.random.RandomClient;
import org.wikipedia.feed.searchbar.SearchClient;
import org.wikipedia.settings.RbSwitch;

class FeedCoordinator extends FeedCoordinatorBase {

    FeedCoordinator(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void buildScript(int age, WikiSite wiki) {
        boolean restBaseEnabled = RbSwitch.INSTANCE.isRestBaseEnabled(wiki);

        if (age == 0) {
            addPendingClient(new SearchClient());
            if (restBaseEnabled) {
                addPendingClient(new AnnouncementClient());
            }
        }
        if (restBaseEnabled) {
            addPendingClient(new AggregatedFeedContentClient());
        }
        addPendingClient(new ContinueReadingClient());
        if (age == 0) {
            addPendingClient(new MainPageClient());
        }
        addPendingClient(new BecauseYouReadClient());
        if (age == 0) {
            addPendingClient(new RandomClient());
        }
    }
}
