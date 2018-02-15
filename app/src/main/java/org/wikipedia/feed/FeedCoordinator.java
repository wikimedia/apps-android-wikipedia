package org.wikipedia.feed;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.feed.aggregated.AggregatedFeedContentClient;
import org.wikipedia.feed.announcement.AnnouncementClient;
import org.wikipedia.feed.offline.OfflineCardClient;
import org.wikipedia.feed.offline.OfflineCompilationClient;
import org.wikipedia.feed.onboarding.OnboardingClient;
import org.wikipedia.feed.searchbar.SearchClient;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.util.DeviceUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FeedCoordinator extends FeedCoordinatorBase {
    @NonNull private AggregatedFeedContentClient aggregatedClient = new AggregatedFeedContentClient();

    FeedCoordinator(@NonNull Context context) {
        super(context);
        FeedContentType.restoreState();
    }

    @Override
    protected void buildScript(int age) {
        boolean online = DeviceUtil.isOnline();

        conditionallyAddPendingClient(new SearchClient(), age == 0);
        conditionallyAddPendingClient(new OfflineCompilationClient(), age == 0 && !online && OfflineManager.hasCompilation());
        addPendingClient(new OnboardingClient());
        conditionallyAddPendingClient(new AnnouncementClient(), age == 0 && online);

        List<FeedContentType> orderedContentTypes = new ArrayList<>();
        orderedContentTypes.addAll(Arrays.asList(FeedContentType.values()));
        Collections.sort(orderedContentTypes, (FeedContentType a, FeedContentType b)
                -> a.getOrder().compareTo(b.getOrder()));

        for (FeedContentType contentType : orderedContentTypes) {
            addPendingClient(contentType.newClient(aggregatedClient, age, online));
        }

        conditionallyAddPendingClient(new OfflineCardClient(), age == 0 && !online);

    }
}
