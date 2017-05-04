package org.wikipedia.feed.view;

import android.support.annotation.NonNull;

import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestImg;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.SecondaryTestImg;
import org.wikipedia.test.view.SecondaryTestStr;
import org.wikipedia.test.view.TestImg;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.wikipedia.feed.view.ListCardItemView.Callback;

public class ListCardItemViewTest extends ViewTest {
    private ListCardItemView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull PrimaryTestImg image,
                                  @NonNull PrimaryTestStr title, @NonNull SecondaryTestStr subtitle) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, image, title, subtitle);
        snap(subject, image + "_image", title + "_title", subtitle + "_subtitle");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT);
        snap(subject);
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT, SecondaryTestStr.SHORT);
        requestFocus(subject);
        snap(subject);
    }

    // todo: how can we test popupmenu which requires an activity?

    @Theory public void testSetCallback(@TestedOnBool boolean nul) {
        setUpTypical();
        Callback callback = nul ? null : mock(Callback.class);
        subject.setCallback(callback);
        assertThat(subject.getCallback(), is(callback));
    }

    @Test public void testSetImage() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestImg.NULL, PrimaryTestStr.SHORT, SecondaryTestStr.SHORT);

        assertThat(subject.imageView.getController(), nullValue());
        String url = frescoUri(SecondaryTestImg.CHECKERBOARD.id()).toString();
        subject.setImage(url);
        assertThat(subject.imageView.getController(), notNullValue());
    }

    @Theory public void testSetTitle(@TestedOnBool boolean nul) {
        setUpTypical();
        String text = nul ? null : "text";
        subject.setTitle(text);
        assertThat(subject.titleView.getText().toString(), is(defaultString(text)));
    }

    @Theory public void testSetSubtitle(@TestedOnBool boolean nul) {
        setUpTypical();
        String text = nul ? null : "Text";
        subject.setSubtitle(text);
        assertThat(subject.subtitleView.getText().toString(), is(defaultString(text)));
    }

    @Theory public void testSetSubtitleCapitalization() {
        setUpTypical();
        subject.setSubtitle("text");
        assertThat(subject.subtitleView.getText().toString(), is("Text"));
    }

    private void setUpTypical() {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestImg.NONNULL, PrimaryTestStr.SHORT, SecondaryTestStr.SHORT);
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestImg image,
                       @NonNull TestStr title, @NonNull TestStr subtitle) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new ListCardItemView(ctx());
        if (!image.isNull()) {
            subject.setImage(frescoUri(image.id()).toString());
        }
        subject.setTitle(str(title));
        subject.setSubtitle(str(subtitle));
    }
}
