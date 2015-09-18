package org.wikipedia.richtext;

import android.graphics.Paint;

public class LeadingSpan extends RelativeLineHeightSpan {
    public LeadingSpan(float scalar) {
        super(scalar);
    }

    @Override
    public void chooseHeight(CharSequence text,
                             int start,
                             int end,
                             int istartv,
                             int v,
                             Paint.FontMetricsInt metrics) {
        // Don't change the state of metrics until all calculations are performed.
        int scaledAscender = (int) scaledAscender(metrics);
        int scaledDescender = (int) scaledDescender(metrics);

        metrics.ascent = scaledAscender;
        metrics.descent = scaledDescender;
    }
}