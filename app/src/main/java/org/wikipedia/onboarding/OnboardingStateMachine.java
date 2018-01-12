package org.wikipedia.onboarding;

public interface OnboardingStateMachine {
    boolean isTocTutorialEnabled();
    void setTocTutorial();
    boolean isSelectTextTutorialEnabled();
    void setSelectTextTutorial();
    boolean isShareTutorialEnabled();
    void setShareTutorial();
    boolean isReadingListTutorialEnabled();
    void setReadingListTutorial();
    void setDescriptionEditTutorial();
}
