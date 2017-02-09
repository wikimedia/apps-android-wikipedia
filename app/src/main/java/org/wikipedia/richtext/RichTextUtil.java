package org.wikipedia.richtext;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.widget.TextView;

import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RichTextUtil {
    @NonNull public static Spannable setSpans(@NonNull Spannable spannable,
                                              int start,
                                              int end,
                                              int flags,
                                              @NonNull Object... spans) {
        for (Object span : spans) {
            spannable.setSpan(span, start, end, flags);
        }
        return spannable;
    }

    /**
     * Apply only the spans from src to dst specific by spans.
     *
     * @see {@link android.text.TextUtils#copySpansFrom}
     */
    public static void copySpans(@NonNull Spanned src,
                                 @NonNull Spannable dst,
                                 @NonNull Collection<Object> spans) {
        for (Object span : spans) {
            int start = src.getSpanStart(span);
            int end = src.getSpanEnd(span);
            int flags = src.getSpanFlags(span);
            dst.setSpan(span, start, end, flags);
        }
    }

    /** Strips all rich text except spans used to provide compositional hints. */
    public static CharSequence stripRichText(CharSequence str, int start, int end) {
        String plainText = str.toString();
        SpannableString ret = new SpannableString(plainText);
        if (str instanceof Spanned) {
            List<Object> keyboardHintSpans = getComposingSpans((Spanned) str, start, end);
            copySpans((Spanned) str, ret, keyboardHintSpans);
        }
        return ret;
    }

    /**
     * @return Temporary spans, often applied by the keyboard to provide hints such as typos.
     *
     * @see {@link android.view.inputmethod.BaseInputConnection#removeComposingSpans}
     * @see {@link android.inputmethod.latin.inputlogic.InputLogic#setComposingTextInternalWithBackgroundColor}
     */
    @NonNull public static List<Object> getComposingSpans(@NonNull Spanned spanned,
                                                          int start,
                                                          int end) {
        // TODO: replace with Apache CollectionUtils.filter().
        List<Object> ret = new ArrayList<>();
        for (Object span : getSpans(spanned, start, end)) {
            if (isComposingSpan(spanned, span)) {
                ret.add(span);
            }
        }
        return ret;
    }

    public static Object[] getSpans(@NonNull Spanned spanned, int start, int end) {
        Class<Object> anyType = Object.class;
        return spanned.getSpans(start, end, anyType);
    }

    public static boolean isComposingSpan(@NonNull Spanned spanned, Object span) {
        return isFlaggedSpan(spanned, span, Spanned.SPAN_COMPOSING);
    }

    public static boolean isFlaggedSpan(@NonNull Spanned spanned, Object span, int flags) {
        return (spanned.getSpanFlags(span) & flags) == flags;
    }

    public static void removeUnderlinesFromLinks(@NonNull TextView textView) {
        CharSequence text = textView.getText();
        if (text instanceof Spanned) {
            Spannable spannable = new SpannableString(text);
            removeUnderlinesFromLinks(spannable, spannable.getSpans(0, spannable.length(), URLSpan.class));
            textView.setText(spannable);
        }
    }

    public static void removeUnderlinesFromLinks(@NonNull Spannable spannable,
                                                 @NonNull URLSpan[] spans) {
        for (URLSpan span: spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            spannable.setSpan(span, start, end, 0);
        }
    }

    public static void removeUnderlinesFromLinksAndMakeBold(@NonNull TextView textView) {
        CharSequence text = textView.getText();
        if (text instanceof Spanned) {
            Spannable spannable = new SpannableString(text);
            removeUnderlinesFromLinksAndMakeBold(spannable, spannable.getSpans(0, spannable.length(), URLSpan.class));
            textView.setText(spannable);
        }
    }

    public static void removeUnderlinesFromLinksAndMakeBold(@NonNull Spannable spannable,
                                                 @NonNull URLSpan[] spans) {
        for (URLSpan span: spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new URLSpanBoldNoUnderline(span.getURL());
            spannable.setSpan(span, start, end, 0);
        }
    }

    public static String stripHtml(@NonNull String html) {
        return StringUtil.fromHtml(html).toString();
    }


    public static CharSequence remove(@NonNull CharSequence text, @IntRange(from = 1) int start, int end) {
        return new SpannedString(TextUtils.concat(text.subSequence(0, start - 1),
                text.subSequence(end, text.length())));
    }

    private RichTextUtil() { }
}
