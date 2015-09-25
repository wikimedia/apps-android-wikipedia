package org.wikipedia.richtext;

import android.support.annotation.NonNull;
import android.text.SpannableString;

public final class RichTextUtil {
    @NonNull public static SpannableString setSpans(@NonNull SpannableString str,
                                                    int start,
                                                    int end,
                                                    int flags,
                                                    @NonNull Object... spans) {
        for (Object span : spans) {
            str.setSpan(span, start, end, flags);
        }
        return str;
    }

    private RichTextUtil() { }
}