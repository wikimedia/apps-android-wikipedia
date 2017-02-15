package org.wikipedia.edit.richtext;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.NonNull;

import org.wikipedia.R;

import static android.support.v4.content.ContextCompat.getColor;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;

public enum SyntaxRuleStyle {
    TEMPLATE {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            int color = getColor(ctx, getThemedAttributeId(ctx, R.attr.syntax_highlight_template_color));
            return new ColorSpanEx(color, Color.TRANSPARENT, spanStart, syntaxItem);
        }
    },
    INTERNAL_LINK {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            int color = getColor(ctx, getThemedAttributeId(ctx, R.attr.link_color));
            return new ColorSpanEx(color, Color.TRANSPARENT, spanStart, syntaxItem);
        }
    },
    EXTERNAL_LINK {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            int color = getColor(ctx, getThemedAttributeId(ctx, R.attr.link_color));
            return new ColorSpanEx(color, Color.TRANSPARENT, spanStart, syntaxItem);
        }
    },
    REF {
        @NonNull @Override public SpanExtents createSpan(@NonNull Context ctx, int spanStart,
                                                         SyntaxRule syntaxItem) {
            return new ColorSpanEx(getColor(ctx, R.color.dark_green), Color.TRANSPARENT, spanStart,
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
