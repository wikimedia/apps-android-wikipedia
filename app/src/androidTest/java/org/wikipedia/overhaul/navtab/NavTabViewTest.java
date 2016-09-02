package org.wikipedia.overhaul.navtab;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

public class NavTabViewTest extends ViewTest {
    private NavTabView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_XL, WIDTH_DP_XS}) int widthDp) {
        setUp(widthDp, LayoutDirection.LOCALE, Theme.LIGHT, Select.DESELECTED);
        snap(subject);
    }

    @Theory public void testLayoutDirection(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_XS}) int widthDp,
                                            LayoutDirection direction) {
        setUp(widthDp, direction, Theme.LIGHT, Select.DESELECTED);
        snap(subject);
    }

    @Theory public void testTheme(Theme theme, Select select) {
        setUp(WIDTH_DP_XS, LayoutDirection.LOCALE, theme, select);
        snap(subject, select.name().toLowerCase());
    }

    private void setUp(int widthDp, LayoutDirection layoutDirection, Theme theme, Select select) {
        setUp(widthDp, layoutDirection, theme);

        subject = new NavTabView(ctx())
                .text(NavTab.EXPLORE.text())
                .icon(NavTab.EXPLORE.icon());
        subject.setSelected(select == Select.SELECTED);
    }
}