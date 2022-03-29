package org.wikipedia.edit.richtext

/**
 * Interface for creating Span styles that contain beginning & end information,
 * as well as the syntax rule with which the span is associated.
 */
interface SpanExtents {

    /**
     * Starting position that this span will take.
     * NOTE: This should only be used when initially applying the span. This variable does not get
     * updated when the span is moved around in the Editable string, and should not be used to
     * determine the span's position after setting it.
     */
    var start: Int

    /**
     * Ending position that this span will take.
     * NOTE: This should only be used when initially applying the span. This variable does not get
     * updated when the span is moved around in the Editable string, and should not be used to
     * determine the span's position after setting it.
     */
    var end: Int

    var syntaxRule: SyntaxRule
}
