package org.wikipedia.richtext;

import android.graphics.Paint;
import android.text.Spanned;
import android.text.style.LineHeightSpan;

/*package*/ abstract class RelativeLineHeightSpan implements LineHeightSpan {
    private final float scalar;

    RelativeLineHeightSpan(float scalar) {
        this.scalar = scalar;
    }

    protected float scaledAscender(Paint.FontMetricsInt metrics) {
        return -ascenderHeightRatio(metrics) * scaledLineHeight(metrics);
    }

    protected float scaledDescender(Paint.FontMetricsInt metrics) {
        return descenderHeightRatio(metrics) * scaledLineHeight(metrics);
    }

    protected float descenderHeightRatio(Paint.FontMetricsInt metrics) {
        return 1 - ascenderHeightRatio(metrics);
    }

    protected float ascenderHeightRatio(Paint.FontMetricsInt metrics) {
        int height = lineHeight(metrics);
        return height == 0 ? 0 : Math.abs(metrics.ascent) / (float) height;
    }

    protected float scaledLineHeight(Paint.FontMetricsInt metrics) {
        return lineHeight(metrics) * scalar;
    }

    protected int lineHeight(Paint.FontMetricsInt metrics) {
        return Math.abs(metrics.ascent) + Math.abs(metrics.descent);
    }

    protected boolean spanStart(CharSequence text, int start) {
        return spanStart((Spanned) text, start);
    }

    protected boolean spanStart(Spanned text, int start) {
        return text.getSpanStart(this) == start;
    }

    protected boolean spanEnd(CharSequence text, int end) {
        return spanEnd((Spanned) text, end);
    }

    protected boolean spanEnd(Spanned text, int end) {
        return text.getSpanEnd(this) == end;
    }
}
