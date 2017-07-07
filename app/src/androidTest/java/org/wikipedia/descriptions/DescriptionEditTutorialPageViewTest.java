package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.onboarding.OnboardingPageView;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

public class DescriptionEditTutorialPageViewTest extends ViewTest {
    private OnboardingPageView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_L}) int widthDp, @NonNull FontScale fontScale) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT);
            snap(subject, page.name());
        }
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT);
            snap(subject, page.name());
        }
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
            snap(subject, page.name());
        }
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        for (DescriptionEditTutorialPage page : DescriptionEditTutorialPage.values()) {
            setUp(page, WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
            requestFocus(subject);
            snap(subject, page.name());
        }
    }

    private void setUp(@NonNull DescriptionEditTutorialPage page, int widthDp, @NonNull LayoutDirection layoutDirection, @NonNull FontScale fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);
        LayoutInflater inflater = LayoutInflater.from(ctx());
        subject = (OnboardingPageView) inflater.inflate(page.getLayout(), null, false);
    }
}
