package org.wikipedia.navtab;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@SmallTest public class NavTabViewTest extends ViewTest {
    private NavTabView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_XS}) int widthDp,
                                  @NonNull FontScale fontScale) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT);
        snap(subject);
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_XS, direction, FontScale.DEFAULT, Theme.LIGHT);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
        snap(subject);
    }

    @Theory public void testSelect(@NonNull Theme theme) {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, FontScale.DEFAULT, theme);
        subject.setSelected(true);
        snap(subject);
    }

    @Test public void testText() {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
        assertThat(subject.getText().toString(), is(str(NavTab.EXPLORE.text())));
    }

    @Test public void testIcon() {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT);
        assertThat(subject.getCompoundDrawables(), hasItemInArray(notNullValue()));
    }

    @Override protected void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                                   @NonNull FontScale fontScale, @NonNull Theme theme) {
        super.setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new NavTabView(ctx())
                .text(NavTab.EXPLORE.text())
                .icon(NavTab.EXPLORE.icon());
    }
}