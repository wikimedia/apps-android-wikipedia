package org.wikipedia.richtext;

import android.graphics.Typeface;
import android.text.TextPaint;

public class URLSpanBoldNoUnderline extends URLSpanNoUnderline {
    public URLSpanBoldNoUnderline(String url) {
        super(url);
    }

    @Override public void updateDrawState(TextPaint paint) {
        super.updateDrawState(paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }
}
