package org.wikipedia.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyntaxHighlightableEditTextTest {

    @Test
    fun testHighlightFirstOccurrenceWithoutContext() {
        val wikitext = "The cell divides. Each cell grows."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "cell")
        assertEquals(wikitext.indexOf("cell"), range?.first)
        assertEquals("cell", range?.let { wikitext.substring(it) })
    }

    @Test
    fun testHighlightSecondOccurrenceUsingContext() {
        val wikitext = "The cell divides. Each cell grows."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "cell", "The cell divides. Each ", " grows.")
        assertEquals(wikitext.lastIndexOf("cell"), range?.first)
    }

    @Test
    fun testHighlightUsingOnlyTextAfter() {
        val wikitext = "A tissue forms organs. A tissue can regenerate."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "tissue", "", " can regenerate.")
        assertEquals(wikitext.lastIndexOf("tissue"), range?.first)
    }

    @Test
    fun testHighlightStandaloneWordInsteadOfWordFragment() {
        // "в" also appears inside "Время" and "Москве", which should not win over the standalone word.
        val wikitext = "Время жизни в Москве. Живёт в лесу."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "в", "Время жизни в Москве. Живёт ", " лесу.")
        assertEquals(wikitext.indexOf("в лесу"), range?.first)
        assertEquals(1, range?.count())
    }

    @Test
    fun testHighlightWordFragmentWhenSelectionIsInsideWord() {
        val wikitext = "cat and concatenate"
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "cat", "cat and con", "enate")
        assertEquals(wikitext.indexOf("concatenate") + "con".length, range?.first)
    }

    @Test
    fun testHighlightContextAcrossLinkMarkupAndCitations() {
        val wikitext = "Cells are found in [[Plant|plants]].<ref>{{cite web|title=Cells}}</ref> " +
                "Cells are found in [[Animal|animals]] too."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "Cells",
            "Cells are found in plants.[1] ", " are found in animals too.")
        assertEquals(wikitext.lastIndexOf("Cells"), range?.first)
    }

    @Test
    fun testHighlightMultipleWordsSpanningMarkup() {
        val wikitext = "The [[multicellular organism]] lives. Every [[multicellular organism|multicellular organism]] grows."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "multicellular organism",
            "The multicellular organism lives. Every ", " grows.")
        val expectedStart = wikitext.indexOf("multicellular", wikitext.indexOf("Every"))
        assertEquals(expectedStart, range?.first)
    }

    @Test
    fun testHighlightFallsBackToLastWordWhenSelectionIsNotFound() {
        val wikitext = "Only the word organism exists here."
        val range = SyntaxHighlightableEditText.findHighlightRange(wikitext, "missing organism")
        assertEquals("organism", range?.let { wikitext.substring(it) })
    }

    @Test
    fun testHighlightReturnsNullWhenTextIsAbsent() {
        assertNull(SyntaxHighlightableEditText.findHighlightRange("Some wikitext.", "absent"))
    }

    @Test
    fun testHighlightReturnsNullForBlankText() {
        assertNull(SyntaxHighlightableEditText.findHighlightRange("Some wikitext.", "   "))
    }
}
