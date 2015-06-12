package org.wikipedia.editing.richtext;

import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

public class ColorSpanEx extends ForegroundColorSpan implements SpanExtents {

    public ColorSpanEx(int foreColor, int backColor, int spanStart, SyntaxRule syntaxRule) {
        super(foreColor);
        this.spanStart = spanStart;
        this.syntaxRule = syntaxRule;
        this.backColor = backColor;
    }

    private int spanStart;
    private int spanEnd;
    private SyntaxRule syntaxRule;
    private int backColor;

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
