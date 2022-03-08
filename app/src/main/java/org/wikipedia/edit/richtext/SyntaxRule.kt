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
class SyntaxRule(val startSym: String, val endSym: String, val spanStyle: SyntaxRuleStyle) {
    /**
     * Whether the start symbol is the same as the end symbol
     * (for faster processing)
     */
    val isStartEndSame: Boolean = startSym == endSym

    val startChars = startSym.toCharArray()

    val endChars = endSym.toCharArray()
}
