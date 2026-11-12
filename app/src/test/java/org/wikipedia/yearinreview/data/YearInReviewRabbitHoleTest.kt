package org.wikipedia.yearinreview.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.wikipedia.history.db.HistoryEntryLink

class YearInReviewRabbitHoleTest {

    @Test
    fun `no rabbit hole without any links`() {
        assertNull(YearInReviewRabbitHole.longestOf(emptyList()))
    }

    @Test
    fun `no rabbit hole when no entry was reached from another`() {
        val links = listOf(
            HistoryEntryLink(id = 1, prevId = -1),
            HistoryEntryLink(id = 2, prevId = -1)
        )
        assertNull(YearInReviewRabbitHole.longestOf(links))
    }

    @Test
    fun `the longest chain wins`() {
        val links = listOf(
            HistoryEntryLink(id = 1, prevId = -1),
            HistoryEntryLink(id = 2, prevId = 1),
            HistoryEntryLink(id = 3, prevId = 2),
            HistoryEntryLink(id = 4, prevId = -1),
            HistoryEntryLink(id = 5, prevId = 4)
        )
        assertEquals(YearInReviewRabbitHole(startEntryId = 1, endEntryId = 3, articleCount = 3), YearInReviewRabbitHole.longestOf(links))
    }

    @Test
    fun `the longest branch of a chain wins`() {
        val links = listOf(
            HistoryEntryLink(id = 1, prevId = -1),
            HistoryEntryLink(id = 2, prevId = 1),
            HistoryEntryLink(id = 3, prevId = 2),
            HistoryEntryLink(id = 4, prevId = 2),
            HistoryEntryLink(id = 5, prevId = 4),
            HistoryEntryLink(id = 6, prevId = 1)
        )
        assertEquals(YearInReviewRabbitHole(startEntryId = 1, endEntryId = 5, articleCount = 4), YearInReviewRabbitHole.longestOf(links))
    }

    @Test
    fun `a link to an entry outside of the given links starts a new chain`() {
        val links = listOf(
            HistoryEntryLink(id = 10, prevId = 7),
            HistoryEntryLink(id = 11, prevId = 10)
        )
        assertEquals(YearInReviewRabbitHole(startEntryId = 10, endEntryId = 11, articleCount = 2), YearInReviewRabbitHole.longestOf(links))
    }

    @Test
    fun `a tie goes to the more recent rabbit hole`() {
        val links = listOf(
            HistoryEntryLink(id = 1, prevId = -1),
            HistoryEntryLink(id = 2, prevId = 1),
            HistoryEntryLink(id = 3, prevId = -1),
            HistoryEntryLink(id = 4, prevId = 3)
        )
        assertEquals(YearInReviewRabbitHole(startEntryId = 3, endEntryId = 4, articleCount = 2), YearInReviewRabbitHole.longestOf(links))
    }

    @Test
    fun `links are followed regardless of their order`() {
        val links = listOf(
            HistoryEntryLink(id = 3, prevId = 2),
            HistoryEntryLink(id = 1, prevId = -1),
            HistoryEntryLink(id = 2, prevId = 1)
        )
        assertEquals(YearInReviewRabbitHole(startEntryId = 1, endEntryId = 3, articleCount = 3), YearInReviewRabbitHole.longestOf(links))
    }

    @Test
    fun `a cycle of links doesn't continue a chain forever`() {
        val links = listOf(
            HistoryEntryLink(id = 1, prevId = 2),
            HistoryEntryLink(id = 2, prevId = 1)
        )
        assertEquals(YearInReviewRabbitHole(startEntryId = 1, endEntryId = 2, articleCount = 2), YearInReviewRabbitHole.longestOf(links))
    }
}
