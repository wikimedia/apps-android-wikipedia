package org.wikipedia.yearinreview.data

import org.wikipedia.history.db.HistoryEntryLink

data class YearInReviewRabbitHole(
    val startEntryId: Long,
    val endEntryId: Long,
    val articleCount: Int
) {
    companion object {
        const val MIN_ARTICLES = 2

        // Entries that were reached from the same entry branch off from it, so the longest rabbit hole is the longest chain
        // from any entry back to the first entry it was reached from. A link to an entry that isn't among the given links
        // starts a new chain. A tie goes to the more recent rabbit hole. Null when no chain has at least MIN_ARTICLES.
        fun longestOf(links: List<HistoryEntryLink>): YearInReviewRabbitHole? {
            val rabbitHolesByEndEntryId = mutableMapOf<Long, YearInReviewRabbitHole>()
            // An entry is always reached from an older one, which has a lower id, so going through them by id builds each chain
            // from its start. This also ignores any link that points forward, so that a cycle can't continue a chain forever.
            links.sortedBy { it.id }.forEach { link ->
                val previousRabbitHole = rabbitHolesByEndEntryId[link.prevId]
                rabbitHolesByEndEntryId[link.id] = previousRabbitHole?.copy(endEntryId = link.id, articleCount = previousRabbitHole.articleCount + 1)
                    ?: YearInReviewRabbitHole(startEntryId = link.id, endEntryId = link.id, articleCount = 1)
            }
            return rabbitHolesByEndEntryId.values
                .filter { it.articleCount >= MIN_ARTICLES }
                .maxWithOrNull(compareBy<YearInReviewRabbitHole> { it.articleCount }.thenBy { it.endEntryId })
        }
    }
}
