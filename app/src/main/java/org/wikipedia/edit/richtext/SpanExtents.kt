package org.wikipedia.edit.richtext

/**
 * Interface for creating Span styles that contain beginning & end information,
 * as well as the syntax rule with which the span is associated.
 */
interface SpanExtents {
    var start: Int
    var end: Int
    var syntaxRule: SyntaxRule
}
