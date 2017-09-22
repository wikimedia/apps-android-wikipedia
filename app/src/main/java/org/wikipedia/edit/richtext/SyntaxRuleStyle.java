package org.wikipedia.edit.richtext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import org.wikipedia.R;

import static android.support.v4.content.ContextCompat.getColor;
import static org.wikipedia.util.ResourceUtil.getThemedColor;

public enum SyntaxRuleStyle {
    TEMPLATE {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            @ColorInt int color = getThemedColor(ctx, R.attr.material_theme_secondary_color);
            return new ColorSpanEx(color, Color.TRANSPARENT, spanStart, syntaxItem);
        }
    },
    INTERNAL_LINK {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            @ColorInt int color = getThemedColor(ctx, R.attr.colorAccent);
            return new ColorSpanEx(color, Color.TRANSPARENT, spanStart, syntaxItem);
        }
    },
    EXTERNAL_LINK {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            @ColorInt int color = getThemedColor(ctx, R.attr.colorAccent);
            return new ColorSpanEx(color, Color.TRANSPARENT, spanStart, syntaxItem);
        }
    },
    REF {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            return new ColorSpanEx(getColor(ctx, R.color.green30), Color.TRANSPARENT, spanStart,
                    syntaxItem);
        }
    },
    BOLD_ITALIC {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            return new StyleSpanEx(Typeface.BOLD_ITALIC, spanStart, syntaxItem);
        }
    },
    BOLD {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            return new StyleSpanEx(Typeface.BOLD, spanStart, syntaxItem);
        }
    },
    ITALIC {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            return new StyleSpanEx(Typeface.ITALIC, spanStart, syntaxItem);
        }
    };

    @NonNull public abstract SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                    SyntaxRule syntaxItem);
}
