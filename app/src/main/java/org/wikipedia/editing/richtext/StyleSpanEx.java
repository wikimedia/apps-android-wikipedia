package org.wikipedia.editing.richtext;

import android.text.style.StyleSpan;

public class StyleSpanEx extends StyleSpan implements SpanExtents {

    public StyleSpanEx(int style, int spanStart, SyntaxRule syntaxRule) {
        super(style);
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
