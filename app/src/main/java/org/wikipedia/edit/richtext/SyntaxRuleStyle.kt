package org.wikipedia.edit.richtext

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil

enum class SyntaxRuleStyle {
    TEMPLATE {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(ResourceUtil.getThemedColor(ctx, R.attr.placeholder_color), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    INTERNAL_LINK {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(ResourceUtil.getThemedColor(ctx, R.attr.progressive_color), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    EXTERNAL_LINK {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(ResourceUtil.getThemedColor(ctx, R.attr.progressive_color), Color.TRANSPARENT, spanStart, syntaxItem)
        }
    },
    REF {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(ResourceUtil.getThemedColor(ctx, R.attr.success_color), Color.TRANSPARENT, spanStart, syntaxItem)
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
    UNDERLINE {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return UnderlineSpanEx(spanStart, syntaxItem)
        }
    },
    STRIKETHROUGH {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return StrikethroughSpanEx(spanStart, syntaxItem)
        }
    },
    TEXT_LARGE {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return RelativeSizeSpanEx(1.2f, spanStart, syntaxItem)
        }
    },
    TEXT_SMALL {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return RelativeSizeSpanEx(0.8f, spanStart, syntaxItem)
        }
    },
    CODE {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return TypefaceSpanEx(Typeface.MONOSPACE, spanStart, syntaxItem)
        }
    },
    SUPERSCRIPT {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return SuperscriptSpanEx(spanStart, syntaxItem)
        }
    },
    SUBSCRIPT {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return SubscriptSpanEx(spanStart, syntaxItem)
        }
    },
    HEADING_LARGE {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return RelativeSizeSpanEx(1.3f, spanStart, syntaxItem)
        }
    },
    HEADING_MEDIUM {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return RelativeSizeSpanEx(1.2f, spanStart, syntaxItem)
        }
    },
    HEADING_SMALL {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return RelativeSizeSpanEx(1.1f, spanStart, syntaxItem)
        }
    },
    SEARCH_MATCHES {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(Color.BLACK, ResourceUtil.getThemedColor(ctx, R.attr.highlight_color), spanStart, syntaxItem)
        }
    },
    SEARCH_MATCH_SELECTED {
        override fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents {
            return ColorSpanEx(Color.BLACK, ResourceUtil.getThemedColor(ctx, R.attr.focus_color), spanStart, syntaxItem)
        }
    };

    abstract fun createSpan(ctx: Context, spanStart: Int, syntaxItem: SyntaxRule): SpanExtents
}
