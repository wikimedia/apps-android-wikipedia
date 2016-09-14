package org.wikipedia.navtab;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@SmallTest public class NavTabViewTest extends ViewTest {
    private NavTabView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_XS}) int widthDp,
                                  float fontScale) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT);
        snap(subject);
    }

    @Theory public void testLayoutDirection(LayoutDirection direction) {
        setUp(WIDTH_DP_XS, direction, 1, Theme.LIGHT);
        snap(subject);
    }

    @Theory public void testTheme(Theme theme) {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, 1, theme);
        snap(subject);
    }

    @Theory public void testSelect(Theme theme) {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, 1, theme);
        subject.setSelected(true);
        snap(subject);
    }

    @Test public void testText() {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, 1, Theme.LIGHT);
        assertThat(subject.getText().toString(), is(str(NavTab.EXPLORE.text())));
    }

    @Test public void testIcon() {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, 1, Theme.LIGHT);
        assertThat(subject.getCompoundDrawables(), hasItemInArray(notNullValue()));
    }

    @Override protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                                   float fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new NavTabView(ctx())
                .text(NavTab.EXPLORE.text())
                .icon(NavTab.EXPLORE.icon());
    }
}