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
    val startChars = startSymbol.toCharArray()
    val endChars = endSymbol.toCharArray()
}
