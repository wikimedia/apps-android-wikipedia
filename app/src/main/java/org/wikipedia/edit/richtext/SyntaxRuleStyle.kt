package org.wikipedia.edit.richtext

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil.getThemedColor

enum class SyntaxRuleStyle {
    TEMPLATE {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(getThemedColor(ctx, R.attr.secondary_text_color), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    INTERNAL_LINK {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(getThemedColor(ctx, R.attr.colorAccent), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    EXTERNAL_LINK {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(getThemedColor(ctx, R.attr.colorAccent), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    REF {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(getThemedColor(ctx, R.attr.green_highlight_color), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    BOLD_ITALIC {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return StyleSpanEx(Typeface.BOLD_ITALIC, spanStart, syntaxItem)
        }
    },
    BOLD {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return StyleSpanEx(Typeface.BOLD, spanStart, syntaxItem)
        }
    },
    ITALIC {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return StyleSpanEx(Typeface.ITALIC, spanStart, syntaxItem)
        }
    },
    SEARCH_MATCHES {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(Color.BLACK, ContextCompat.getColor(ctx, R.color.find_in_page), spanStart, syntaxItem)
        }
    },
    SEARCH_MATCH_SELECTED {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(Color.BLACK, ContextCompat.getColor(ctx, R.color.find_in_page_active), spanStart, syntaxItem)
        }
    };

    abstract fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents
}
