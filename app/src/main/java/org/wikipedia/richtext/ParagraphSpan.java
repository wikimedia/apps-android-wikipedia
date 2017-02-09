package org.wikipedia.richtext;

import android.graphics.Paint;

public class ParagraphSpan extends RelativeLineHeightSpan {
    public ParagraphSpan(float scalar) {
        super(scalar);
    }

    @Override
    public void chooseHeight(CharSequence text,
                             int start,
                             int end,
                             int istartv,
                             int v,
                             Paint.FontMetricsInt metrics) {
        if (spanStart(text, start)) {
            metrics.ascent = (int) scaledAscender(metrics);
        } else if (spanEnd(text, end)) {
            metrics.descent = (int) scaledDescender(metrics);
        }
    }
}
