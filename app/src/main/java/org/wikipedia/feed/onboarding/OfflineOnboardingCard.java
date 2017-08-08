package org.wikipedia.feed.onboarding;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.ReleaseUtil;

public class OfflineOnboardingCard extends OnboardingCard {
    @NonNull @Override public CardType type() {
        return CardType.ONBOARDING_OFFLINE;
    }

    @Override public boolean shouldShow() {
        // TODO: remove pre-beta flag when ready.
        return super.shouldShow() && ReleaseUtil.isPreBetaRelease() && DeviceUtil.isOnline() && !OfflineManager.hasCompilation();
    }

    @Override public int prefKey() {
        return R.string.preference_key_offline_onboarding_card_enabled;
    }

    @NonNull @Override public OnboardingAction action() {
        return OnboardingAction.OFFLINE_LIBRARY;
    }

    @Override public int positiveText() {
        return R.string.offline_library_onboarding_action;
    }

    @Override public int headerText() {
        return R.string.offline_library_title;
    }

    @Override public int headerImage() {
        return R.drawable.ic_offline_white_24dp;
    }

    @NonNull @Override public Uri fullImage() {
        return Uri.parse("https://upload.wikimedia.org/wikipedia/commons/1/1c/Illustration_offline_onboarding.png");
    }
}
