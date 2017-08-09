package org.wikipedia.feed.onboarding;

import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.wikipedia.feed.model.Card;

public abstract class OnboardingCard extends Card {
    public enum OnboardingAction {
        OFFLINE_LIBRARY
    }

    @NonNull public abstract OnboardingAction action();

    @StringRes public abstract int positiveText();

    @StringRes public abstract int headerText();

    @DrawableRes public abstract int headerImage();

    @NonNull public abstract Uri fullImage();
}
