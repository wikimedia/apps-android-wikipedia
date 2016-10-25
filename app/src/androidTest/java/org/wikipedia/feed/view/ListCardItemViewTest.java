package org.wikipedia.feed.view;

import android.net.Uri;

import org.junit.Before;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.ViewTest;
import org.wikipedia.theme.Theme;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.wikipedia.test.ViewTest.LayoutDirection.LOCALE;
import static org.wikipedia.util.StringUtil.emptyIfNull;

public class ListCardItemViewTest extends ViewTest {
    private ListCardItemView subject;

    @Before public void setUp() {
        setUp(WIDTH_DP_S, LOCALE, FONT_SCALES[0], Theme.LIGHT);
        subject = new ListCardItemView(ctx());
    }

    @Theory public void testSetImage(@TestedOn(ints = {0, 1}) int nonnull) {
        Uri uri = nonnull == 0 ? null : mock(Uri.class);
        assertThat(subject.imageView.getController(), nullValue());
        subject.setImage(uri);
        assertThat(subject.imageView.getController(), notNullValue());
    }

    @Theory public void testSetTitle(@TestedOn(ints = {0, 1}) int nonnull) {
        CharSequence title = nonnull == 0 ? null : "subtitle";
        subject.setTitle(title);
        assertThat(subject.titleView.getText(), is(emptyIfNull(title)));
    }

    @Theory public void testSetSubtitle(@TestedOn(ints = {0, 1}) int nonnull) {
        CharSequence subtitle = nonnull == 0 ? null : "subtitle";
        subject.setSubtitle(subtitle);
        assertThat(subject.subtitleView.getText(), is(emptyIfNull(subtitle)));
    }
}