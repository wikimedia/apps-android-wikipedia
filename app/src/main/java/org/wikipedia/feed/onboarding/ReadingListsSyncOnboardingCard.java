package org.wikipedia.feed.onboarding;


import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.model.CardType;

public class ReadingListsSyncOnboardingCard extends OnboardingCard {
    public ReadingListsSyncOnboardingCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.ONBOARDING_READING_LIST_SYNC;
    }

    public boolean shouldShow() {
        return super.shouldShow() && !AccountUtil.isLoggedIn();
    }

    @Override
    public int prefKey() {
        return R.string.preference_key_feed_readinglists_sync_onboarding_card_enabled;
    }
}
