package org.wikipedia.edit.richtext;

import org.wikipedia.model.BaseModel;

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
public class SyntaxRule extends BaseModel {
    private final String startSymbol;
    private final String endSymbol;
    private final String searchText;
    private final SyntaxRuleStyle spanStyle;
    private final boolean sameStartEnd;

    public String getStartSymbol() {
        return startSymbol;
    }

    public String getEndSymbol() {
        return endSymbol;
    }

    public String getSearchText() {
        return searchText;
    }

    public SyntaxRuleStyle getSpanStyle() {
        return spanStyle;
    }

    /**
     * Whether the start symbol is the same as the end symbol
     * (for faster processing)
     */
    public boolean isStartEndSame() {
        return sameStartEnd;
    }

    public boolean hasSearchText() {
        return searchText != null;
    }

    public SyntaxRule(String startSymbol, String endSymbol, SyntaxRuleStyle spanStyle) {
        this.startSymbol = startSymbol;
        this.endSymbol = endSymbol;
        this.spanStyle = spanStyle;
        this.searchText = null;
        sameStartEnd = startSymbol.equals(endSymbol);
    }

    public SyntaxRule(String searchText, SyntaxRuleStyle spanStyle) {
        this.startSymbol = null;
        this.endSymbol = null;
        this.spanStyle = spanStyle;
        this.searchText = searchText;
        sameStartEnd = false;

    }
}
