package org.wikipedia.feed.onboarding;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.ReleaseUtil;

public class OfflineOnboardingCard extends OnboardingCard {
    public OfflineOnboardingCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @NonNull @Override public CardType type() {
        return CardType.ONBOARDING_OFFLINE;
    }

    public boolean shouldShow() {
        // TODO: remove pre-beta flag when ready.
        return super.shouldShow() && ReleaseUtil.isPreBetaRelease() && DeviceUtil.isOnline() && !OfflineManager.hasCompilation();
    }

    @Override public int prefKey() {
        return R.string.preference_key_offline_onboarding_card_enabled;
    }
}
