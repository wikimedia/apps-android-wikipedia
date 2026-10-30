package org.wikipedia.search.interleave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultsInterleaverTest {
    @Test
    fun testMergeVersionMatchesSharedVectors() {
        assertEquals(SearchResultsInterleaver.MERGE_VERSION, TeamDraftMergeVectors.load().mergeVersion)
    }

    @Test
    fun testMergeMatchesSharedVectors() {
        TeamDraftMergeVectors.load().vectors.forEach { vector ->
            val result = SearchResultsInterleaver.merge(vector.lexical, vector.semantic, vector.listLength, vector.seed.toULong()) { it }
            val slots = result.slots.map {
                TeamDraftMergeVectors.ExpectedSlot(it.item, it.searchType.name.lowercase(), it.rank, it.alsoInOther)
            }
            assertEquals(vector.name, vector.expected.coinFlips, result.coinFlipsLog)
            assertEquals(vector.name, vector.expected.slots, slots)
            assertEquals(vector.name, vector.seed, result.seedLog)
        }
    }

    @Test
    fun testMergeIsDeterministicForSameInputs() {
        val lexical = listOf(1L, 2L, 3L, 4L, 5L)
        val semantic = listOf(3L, 6L, 1L, 7L, 8L)
        val seed = SearchResultsInterleaver.newSeed()
        assertEquals(
            SearchResultsInterleaver.merge(lexical, semantic, 8, seed) { it },
            SearchResultsInterleaver.merge(lexical, semantic, 8, seed) { it }
        )
    }

    @Test
    fun testEachRetrieverPicksFirstAboutHalfTheTime() {
        val lexical = (1L..10L).toList()
        val semantic = (101L..110L).toList()
        val listLength = 10
        val lexicalCountByPosition = IntArray(listLength)
        var lexicalFirstFlips = 0
        var totalFlips = 0

        for (seed in 0uL until SIMULATION_RUNS.toULong()) {
            val result = SearchResultsInterleaver.merge(lexical, semantic, listLength, seed) { it }
            lexicalFirstFlips += result.coinFlips.count { it == SearchType.LEXICAL }
            totalFlips += result.coinFlips.size
            result.slots.forEachIndexed { position, slot ->
                if (slot.searchType == SearchType.LEXICAL) {
                    lexicalCountByPosition[position]++
                }
            }
        }

        assertShareIsBalanced("lexical-first share", lexicalFirstFlips.toDouble() / totalFlips)
        lexicalCountByPosition.forEachIndexed { position, count ->
            assertShareIsBalanced("lexical share at position $position", count.toDouble() / SIMULATION_RUNS)
        }
    }

    private fun assertShareIsBalanced(label: String, share: Double) {
        assertTrue("$label was $share", share in 0.49..0.51)
    }

    companion object {
        private const val SIMULATION_RUNS = 100_000
    }
}
