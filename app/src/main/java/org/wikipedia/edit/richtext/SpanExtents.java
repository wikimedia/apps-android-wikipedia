package org.wikipedia.edit.richtext;

/**
 * Interface for creating Span styles that contain beginning & end information,
 * as well as the syntax rule with which the span is associated.
 */
public interface SpanExtents {

    int getStart();

    void setStart(int start);

    int getEnd();

    void setEnd(int end);

    SyntaxRule getSyntaxRule();

    void setSyntaxRule(SyntaxRule item);

}
