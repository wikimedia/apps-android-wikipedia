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

public class DescriptionEditSuccessViewTest extends ViewTest {
    private DescriptionEditSuccessView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_L}) int widthDp, @NonNull FontScale fontScale) {
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
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
        subject = new DescriptionEditSuccessView(ctx());
        DescriptionEditSuccessView.Callback callback = nul ? null : mock(DescriptionEditSuccessView.Callback.class);
        subject.setCallback(callback);

        subject.onDismissClick();
        if (callback != null) {
            verify(callback).onDismissClick();
        }
    }

    @Override protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection, @NonNull FontScale fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);
        subject = new DescriptionEditSuccessView(ctx());
    }
}
