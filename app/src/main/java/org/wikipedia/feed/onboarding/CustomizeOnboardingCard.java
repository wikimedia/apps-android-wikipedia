package org.wikipedia.feed.onboarding;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.util.DeviceUtil;

public class CustomizeOnboardingCard extends OnboardingCard {
    public CustomizeOnboardingCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.ONBOARDING_CUSTOMIZE_FEED;
    }

    public boolean shouldShow() {
        return super.shouldShow() && DeviceUtil.isOnline();
    }

    @Override public int prefKey() {
        return R.string.preference_key_feed_customize_onboarding_card_enabled;
    }
}
