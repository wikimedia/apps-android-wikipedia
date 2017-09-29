package org.wikipedia.richtext;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.junit.experimental.theories.Theory;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.wikipedia.test.view.FontScale;
import org.wikipedia.test.view.LayoutDirection;
import org.wikipedia.test.view.PrimaryTestStr;
import org.wikipedia.test.view.TestStr;
import org.wikipedia.test.view.ViewTest;
import org.wikipedia.theme.Theme;
import org.wikipedia.views.AppTextView;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

public class LeadingSpanTest extends ViewTest {
    private TextView textView;

    @Theory public void testWidth(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                  @NonNull FontScale fontScale, @NonNull PrimaryTestStr text) {
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, text);
        snap(textView, text + "_text");
    }

    @Theory public void testLayoutDirection(@NonNull LayoutDirection direction) {
        setUp(WIDTH_DP_L, direction, FontScale.DEFAULT, Theme.LIGHT, PrimaryTestStr.SHORT);
        snap(textView);
    }

    @Theory public void testLeading(@TestedOn(ints = {WIDTH_DP_L, WIDTH_DP_M}) int widthDp,
                                    @NonNull FontScale fontScale,
                                    @TestedOn(ints = {1, 2}) int leadingScalar) {
        final String str = StringUtils.repeat("Mm%Z@OQW|Pbdpqg ", 100);
        setUp(widthDp, LayoutDirection.LOCALE, fontScale, Theme.LIGHT, str, leadingScalar);
        snap(textView, leadingScalar + "x_leading");
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme, @NonNull TestStr text) {
        setUp(widthDp, layoutDirection, fontScale, theme);
        init(str(text), 1);
    }

    private void setUp(int widthDp, @NonNull LayoutDirection layoutDirection,
                       @NonNull FontScale fontScale, @NonNull Theme theme,
                       @Nullable CharSequence text, float leadingScalar) {
        setUp(widthDp, layoutDirection, fontScale, theme);
        init(text, leadingScalar);
    }

    private void init(@Nullable CharSequence text, float leadingScalar) {
        textView = new AppTextView(ctx());
        textView.setText(spanned(text, leadingScalar));
        textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private Spanned spanned(@Nullable CharSequence text, float leadingScalar) {
        int flags = text == null
                ? Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                : Spannable.SPAN_INCLUSIVE_EXCLUSIVE;
        Spannable spannable = new SpannableString(defaultIfEmpty(text, ""));
        spannable.setSpan(new LeadingSpan(leadingScalar), 0, spannable.length(), flags);
        return spannable;
    }
}
