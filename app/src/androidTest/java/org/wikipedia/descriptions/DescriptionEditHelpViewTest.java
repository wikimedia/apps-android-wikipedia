package org.wikipedia.descriptions;

import android.support.annotation.NonNull;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.wikipedia.descriptions.DescriptionEditHelpView.Callback;

public class DescriptionEditHelpViewTest extends ViewTest {
    private DescriptionEditHelpView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_L}) int widthDp,
                                  @NonNull FontScale fontScale) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT);
        snap(subject);
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
        snap(subject);
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
        requestFocus(subject);
        snap(subject);
    }

    @Theory public void testSetCallback(@TestedOnBool boolean nul) {
        defaultSetUp();
        Callback callback = nul ? null : mock(Callback.class);
        subject.setCallback(callback);

        subject.onAboutClick();
        subject.onGuideClick();
        if (callback != null) {
            verify(callback).onAboutClick();
            verify(callback).onGuideClick();
        }
    }

    private void defaultSetUp() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
    }

    @Override
    protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);
        subject = new DescriptionEditHelpView(ctx());
    }
}
