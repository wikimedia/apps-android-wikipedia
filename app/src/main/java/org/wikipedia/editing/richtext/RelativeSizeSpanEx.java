package org.wikipedia.editing.richtext;

import android.text.style.RelativeSizeSpan;

public class RelativeSizeSpanEx extends RelativeSizeSpan implements SpanExtents {

    public RelativeSizeSpanEx(float proportion, int spanStart, SyntaxRule syntaxRule) {
        super(proportion);
        this.spanStart = spanStart;
        this.syntaxRule = syntaxRule;
    }

    private int spanStart;
    private int spanEnd;
    private SyntaxRule syntaxRule;

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
