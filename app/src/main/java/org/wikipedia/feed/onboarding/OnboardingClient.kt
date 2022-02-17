package org.wikipedia.feed.onboarding

import android.content.Context
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import org.wikipedia.settings.Prefs
import org.wikipedia.util.UriUtil

class OnboardingClient : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        FeedCoordinator.postCardsToCallback(cb, listOfNotNull(getCards(context).getOrNull(age)))
    }

    private fun getCards(context: Context): List<Card> {
        val cards = ArrayList<Card>()
        val card: OnboardingCard

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
        card = CustomizeOnboardingCard(
            Announcement(id = "customizeOnboardingCard1",
                text = context.getString(R.string.feed_configure_onboarding_text),
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/3/3b/Announcement_header_for_Explore_Feed_customization.png",
                action = Announcement.Action(context.getString(R.string.feed_configure_onboarding_action), UriUtil.LOCAL_URL_CUSTOMIZE_FEED),
                negativeText = context.getString(R.string.onboarding_got_it)
            )
        )
        if (card.shouldShow() && Prefs.exploreFeedVisitCount <= SHOW_CUSTOMIZE_ONBOARDING_CARD_COUNT) {
            cards.add(card)
        }
        return cards
    }

    override fun cancel() {}

    companion object {
        private const val SHOW_CUSTOMIZE_ONBOARDING_CARD_COUNT = 5
    }
}
