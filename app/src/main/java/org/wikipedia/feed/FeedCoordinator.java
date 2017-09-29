package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.announcement.AnnouncementClient;
import org.wikipedia.feed.becauseyouread.BecauseYouReadClient;
import org.wikipedia.feed.continuereading.ContinueReadingClient;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.feed.offline.OfflineCompilationClient;
import org.wikipedia.feed.onboarding.OnboardingClient;
import org.wikipedia.feed.onthisday.OnThisDayClient;
import org.wikipedia.feed.random.RandomClient;
import org.wikipedia.feed.searchbar.SearchClient;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.util.DeviceUtil;

import static org.wikipedia.util.ReleaseUtil.isPreBetaRelease;

class FeedCoordinator extends FeedCoordinatorBase {

    FeedCoordinator(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void buildScript(int age) {
        boolean online = DeviceUtil.isOnline();

        conditionallyAddPendingClient(new SearchClient(), age == 0);
        conditionallyAddPendingClient(new OfflineCompilationClient(), age == 0 && !online && OfflineManager.hasCompilation() && isPreBetaRelease());
        conditionallyAddPendingClient(new OnboardingClient(), age == 0);
        conditionallyAddPendingClient(new AnnouncementClient(), age == 0 && online);
        conditionallyAddPendingClient(new AggregatedFeedContentClient(), online);
        addPendingClient(new ContinueReadingClient());
        conditionallyAddPendingClient(new OnThisDayClient(), online && isPreBetaRelease());
        conditionallyAddPendingClient(new MainPageClient(), age == 0);
        conditionallyAddPendingClient(new BecauseYouReadClient(), online);
        conditionallyAddPendingClient(new RandomClient(), age == 0);
    }
}
