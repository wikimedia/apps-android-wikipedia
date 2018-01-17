package org.wikipedia.onboarding;

import org.wikipedia.settings.Prefs;

import java.util.concurrent.TimeUnit;

public final class PrefsOnboardingStateMachine implements OnboardingStateMachine {
    private static final PrefsOnboardingStateMachine INSTANCE = new PrefsOnboardingStateMachine();
    private static final long MIN_MINUTES_PER_TUTORIAL = 1;

    private long millisSinceLastTutorial;

    public static PrefsOnboardingStateMachine getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isTocTutorialEnabled() {
        // don't care about time since last tutorial here, since the ToC tooltip
        // is always the first one shown.
        return Prefs.isTocTutorialEnabled();
    }

    @Override
    public void setTocTutorial() {
        Prefs.setTocTutorialEnabled(false);
        updateTimeSinceLastTutorial();
    }

    @Override
    public boolean isSelectTextTutorialEnabled() {
        return minutesSinceLastTutorial() >= MIN_MINUTES_PER_TUTORIAL
               && !Prefs.isTocTutorialEnabled() && Prefs.isSelectTextTutorialEnabled();
    }

    @Override
    public void setSelectTextTutorial() {
        Prefs.setSelectTextTutorialEnabled(false);
        updateTimeSinceLastTutorial();
    }

    @Override
    public boolean isShareTutorialEnabled() {
        // don't care about time since last tutorial here, since the share tooltip is
        // tied to the highlight action.
        return !Prefs.isTocTutorialEnabled()
                && !isSelectTextTutorialEnabled()
                && Prefs.isShareTutorialEnabled();
    }

    @Override
    public void setShareTutorial() {
        Prefs.setShareTutorialEnabled(false);
        updateTimeSinceLastTutorial();
    }

    @Override
    public boolean isReadingListTutorialEnabled() {
        return Prefs.isReadingListTutorialEnabled();
    }

    @Override
    public void setReadingListTutorial() {
        Prefs.setReadingListTutorialEnabled(false);
    }

    @Override
    public void setDescriptionEditTutorial() {
        Prefs.setDescriptionEditTutorialEnabled(false);
    }

    private void updateTimeSinceLastTutorial() {
        millisSinceLastTutorial = System.currentTimeMillis();
    }

    private long minutesSinceLastTutorial() {
        return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - millisSinceLastTutorial);
    }

    private PrefsOnboardingStateMachine() { }
}
