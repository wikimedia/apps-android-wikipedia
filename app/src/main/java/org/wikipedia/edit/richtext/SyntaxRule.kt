package org.wikipedia.edit.richtext

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
class SyntaxRule(startSymbol: String, endSymbol: String, val spanStyle: SyntaxRuleStyle) {
    /**
     * Whether the start symbol is the same as the end symbol
     * (for faster processing)
     */
    val isStartEndSame: Boolean = startSymbol == endSymbol

    val startChars = startSymbol.toCharArray()

    val endChars = endSymbol.toCharArray()
}
