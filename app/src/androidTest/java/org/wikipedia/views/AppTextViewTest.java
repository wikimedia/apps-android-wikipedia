package org.wikipedia.views;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.apache.commons.lang3.StringUtils;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;

public class AppTextViewTest extends ViewTest {
    private AppTextView subject;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull PrimaryTestStr text) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, text);
        snap(subject, text + "_text");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestStr.SHORT);
        snap(subject);
    }

    @Theory public void testLeading(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                    @NonNull FontScale fontScale) {
        final String str = StringUtils.repeat("Mm%Z@OQW|Pbdpqg ", 100);
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, str);
        snap(subject);
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestStr text) {
        setUp(widthDp, layoutDirection, fontScale, theme);
        init(str(text));
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme,
                       @Nullable CharSequence text) {
        setUp(widthDp, layoutDirection, fontScale, theme);
        init(text);
    }

    private void init(@Nullable CharSequence text) {
        subject = new AppTextView(ctx());
        subject.setText(text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            subject.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }
    }
}
