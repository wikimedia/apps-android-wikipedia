package org.wikipedia.editing.richtext;

/**
 * Represents a single syntax highlighting rule.
 *
 * example:   [[ lorem ipsum ]]
 *             |       |      |
 *        startSymbol  |      |
 *                     |  endSymbol
 *                     |
 *                 spanStyle
 */
public class SyntaxRule {
    private final String startSymbol;
    public String getStartSymbol() {
        return startSymbol;
    }

    private final String endSymbol;
    public String getEndSymbol() {
        return endSymbol;
    }

    private final SyntaxRuleStyle spanStyle;
    public SyntaxRuleStyle getSpanStyle() {
        return spanStyle;
    }

    /**
     * Whether the start symbol is the same as the end symbol
     * (for faster processing)
     */
    private final boolean sameStartEnd;
    public boolean isStartEndSame() {
        return sameStartEnd;
    }

    public SyntaxRule(String startSymbol, String endSymbol, SyntaxRuleStyle spanStyle) {
        this.startSymbol = startSymbol;
        this.endSymbol = endSymbol;
        this.spanStyle = spanStyle;
        sameStartEnd = startSymbol.equals(endSymbol);
    }

    /**
     * Interface for creating a Span style when this rule is matched.
     */
    public interface SyntaxRuleStyle {
        SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem);
    }
}
