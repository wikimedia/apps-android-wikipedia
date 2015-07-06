package org.wikipedia.onboarding;

import org.wikipedia.settings.Prefs;

public final class PrefsOnboardingStateMachine implements OnboardingStateMachine {
    private static final PrefsOnboardingStateMachine INSTANCE = new PrefsOnboardingStateMachine();

    private final boolean initialTocTutorialEnabled = Prefs.isTocTutorialEnabled();

    public static PrefsOnboardingStateMachine getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean isTocTutorialEnabled() {
        return Prefs.isTocTutorialEnabled();
    }

    @Override
    public void setTocTutorial() {
        Prefs.setTocTutorialEnabled(false);
    }

    @Override
    public boolean isSelectTextTutorialEnabled() {
        return !initialTocTutorialEnabled && Prefs.isSelectTextTutorialEnabled();
    }

    @Override
    public void setSelectTextTutorial() {
        Prefs.setSelectTextTutorialEnabled(false);
    }

    @Override
    public boolean isShareTutorialEnabled() {
        return !initialTocTutorialEnabled
                && !isSelectTextTutorialEnabled()
                && Prefs.isShareTutorialEnabled();
    }

    @Override
    public void setShareTutorial() {
        Prefs.setShareTutorialEnabled(false);
    }

    private PrefsOnboardingStateMachine() { }
}