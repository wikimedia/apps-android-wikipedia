package org.wikipedia.feed.onboarding;

import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.wikipedia.feed.model.Card;
import org.wikipedia.settings.PrefsIoUtil;

public abstract class OnboardingCard extends Card {
    public enum OnboardingAction {
        OFFLINE_LIBRARY
    }

    @StringRes public abstract int prefKey();

    @NonNull public abstract OnboardingAction action();

    @StringRes public abstract int positiveText();

    @StringRes public abstract int headerText();

    @DrawableRes public abstract int headerImage();

    @NonNull public abstract Uri fullImage();

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
