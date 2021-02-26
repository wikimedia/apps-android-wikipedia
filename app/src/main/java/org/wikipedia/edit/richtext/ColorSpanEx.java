package org.wikipedia.edit.richtext;

import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class ColorSpanEx extends ForegroundColorSpan implements SpanExtents {
    private int spanStart;
    private int spanEnd;
    private SyntaxRule syntaxRule;
    @ColorInt private int backColor;

    public ColorSpanEx(@ColorInt int foreColor, @ColorInt int backColor, int spanStart, SyntaxRule syntaxRule) {
        super(foreColor);
        this.spanStart = spanStart;
        this.syntaxRule = syntaxRule;
        this.backColor = backColor;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint tp) {
        tp.bgColor = backColor;
        super.updateDrawState(tp);
    }

    @Override
    public int getStart() {
        return spanStart;
    }

    @Override
    public void setStart(int start) {
        spanStart = start;
    }

    @Override
    public int getEnd() {
        return spanEnd;
    }

    @Override
    public void setEnd(int end) {
        spanEnd = end;
    }

    @Override
    public SyntaxRule getSyntaxRule() {
        return syntaxRule;
    }

    @Override
    public void setSyntaxRule(SyntaxRule item) {
        syntaxRule = item;
    }
}
