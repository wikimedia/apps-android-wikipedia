package org.wikipedia.feed.view;

import android.support.annotation.NonNull;
import android.view.View;

import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.R;
import org.wikipedia.test.theories.TestedOnBool;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestImg;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.SecondaryTestImg;
import org.wikipedia.test.view.TestImg;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

import static android.view.View.OnClickListener;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CardLargeHeaderViewTest extends ViewTest {
    private CardLargeHeaderView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull SecondaryTestImg image,
                                  @NonNull PrimaryTestStr title) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, image, title);
        snap(subject, image + "_image", title + "_title");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestImg.NONNULL,
                PrimaryTestStr.SHORT);
        snap(subject);
    }

    @Theory public void testTheme(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NULL,
                PrimaryTestStr.SHORT);
        snap(subject);
    }

    @Theory public void testFocus(@NonNull Theme theme) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, theme, PrimaryTestImg.NULL,
                PrimaryTestStr.SHORT);
        requestFocus(subject);
        snap(subject);
    }

    @Theory public void testSetImage(@NonNull SecondaryTestImg image) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT, image,
                PrimaryTestStr.NULL);
        View imageView = subject.findViewById(R.id.view_card_header_large_image);
        assertThat(imageView.getVisibility(), is(image.isNull() ? View.GONE : View.VISIBLE));
    }

    @Theory public void testSetTitle(@NonNull PrimaryTestStr text) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                PrimaryTestImg.NULL, text);
        assertText(subject, R.id.view_card_header_large_title, text);
    }

    @Theory public void testOnClickListener(@TestedOnBool boolean nul) {
        setUp(WIDTH_DP_L, LayoutDirection.LOCALE, FontScale.DEFAULT, Theme.LIGHT,
                SecondaryTestImg.CHECKERBOARD, PrimaryTestStr.SHORT);

        OnClickListener listener = nul ? null : mock(View.OnClickListener.class);
        subject.onClickListener(listener);
        // todo: why doesn't subject.performClick() apply to backgroundView, a child?
        subject.backgroundView.performClick();
        if (listener != null) {
            verify(listener).onClick(any(View.class));
        }
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestImg image,
                       @NonNull TestStr title) {
        setUp(widthDp, layoutDirection, fontScale, theme);

        subject = new CardLargeHeaderView(ctx())
                .setImage(frescoUri(image.id()))
                .setTitle(str(title));
    }
}
