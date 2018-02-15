package org.wikipedia.feed.onboarding;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.dataclient.FeedClient;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.UriUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnboardingClient implements FeedClient {
    @Override public void request(@NonNull Context context, @NonNull WikiSite wiki, int age,
                                  @NonNull FeedClient.Callback cb) {
        List<Card> cards = getCards(context);
        cb.success(age < cards.size() ? Collections.singletonList(cards.get(age)) : Collections.emptyList());
    }

    private List<Card> getCards(@NonNull Context context) {

        ArrayList<Card> cards = new ArrayList<>();
        OnboardingCard card;

        // NOTE: When adding new onboarding cards, please add them to the *beginning* of the list.

        //        card = new OfflineOnboardingCard(new Announcement(
        //                "offlineOnboardingCard1",
        //                context.getString(R.string.offline_library_onboarding_text),
        //                "https://upload.wikimedia.org/wikipedia/commons/5/5b/Illustration-OfflineLibraryPromo2_3x.png",
        //                new Announcement.Action(context.getString(R.string.offline_library_onboarding_action), UriUtil.LOCAL_URL_OFFLINE_LIBRARY),
        //                context.getString(R.string.onboarding_got_it)));
        //        if (card.shouldShow()) {
        //            cards.add(card);
        //        }

        card = new ReadingListsSyncOnboardingCard(new Announcement(
                "readingListssyncCard",
                context.getString(R.string.feed_reading_lists_sync_onboarding_text),
                "https://upload.wikimedia.org/wikipedia/commons/5/53/Reading_list_sync_image.png",
                new Announcement.Action(context.getString(R.string.menu_login), UriUtil.LOCAL_URL_LOGIN),
                context.getString(R.string.onboarding_got_it)));

        if (card.shouldShow()) {
            cards.add(card);
        }

        card = new CustomizeOnboardingCard(new Announcement(
                "customizeOnboardingCard1",
                context.getString(R.string.feed_configure_onboarding_text),
                "https://upload.wikimedia.org/wikipedia/commons/3/3b/Announcement_header_for_Explore_Feed_customization.png",
                new Announcement.Action(context.getString(R.string.feed_configure_onboarding_action), UriUtil.LOCAL_URL_CUSTOMIZE_FEED),
                context.getString(R.string.onboarding_got_it)));
        if (card.shouldShow()) {
            cards.add(card);
        }

        return cards;
    }

    @Override public void cancel() { }
}
