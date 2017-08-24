package org.wikipedia.feed.onboarding;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.feed.announcement.AnnouncementCard;
import org.wikipedia.settings.PrefsIoUtil;

public abstract class OnboardingCard extends AnnouncementCard {
    public OnboardingCard(@NonNull Announcement announcement) {
        super(announcement);
    }

    @StringRes public abstract int prefKey();

    public boolean shouldShow() {
        return PrefsIoUtil.getBoolean(prefKey(), true);
    }

    @Override public void onDismiss() {
        PrefsIoUtil.setBoolean(prefKey(), false);
    }

    @Override public void onRestore() {
        PrefsIoUtil.setBoolean(prefKey(), true);
    }
}
