package org.wikipedia.richtext;

import android.graphics.Paint;
import android.text.Spanned;

public class LeadingSpan extends RelativeLineHeightSpan {
    public LeadingSpan(float scalar) {
        super(scalar);
    }

    @Override public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v,
                                       Paint.FontMetricsInt metrics) {
        // Only operate on the first line (see IconMarginSpan, DrawableMarginSpan). This will affect
        // all following lines through the metrics parameter output
        if (start == ((Spanned) text).getSpanStart(this)) {
            // Don't change the state of metrics until all calculations are performed.
            int scaledAscender = (int) scaledAscender(metrics);
            int scaledDescender = (int) scaledDescender(metrics);

            metrics.ascent = scaledAscender;
            metrics.descent = scaledDescender;
        }
    }
}
