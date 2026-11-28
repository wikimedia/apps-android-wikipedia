package org.wikipedia.search.interleave

import kotlin.random.Random

/**
 * Team-Draft merge of lexical and semantic results, as defined in the Team-Draft merge spec (T441661).
 * Any change to the output for the same inputs must bump MERGE_VERSION and regenerate the shared test vectors.
 */
object SearchResultsInterleaver {
    const val MERGE_VERSION = 1

    fun newSeed(): ULong {
        return Random.nextLong().toULong()
    }

    fun <T> merge(
        lexical: List<T>,
        semantic: List<T>,
        listLength: Int,
        seed: ULong,
        pageId: (T) -> Long
    ): InterleaveResult<T> {
        val random = SplitMix64(seed)
        val placedPageIds = mutableSetOf<Long>()
        val lexicalPicker = SearchTypePicker(SearchType.LEXICAL, lexical, pageId)
        val semanticPicker = SearchTypePicker(SearchType.SEMANTIC, semantic, pageId)
        val slots = mutableListOf<InterleaveSlot<T>>()
        val coinFlips = mutableListOf<SearchType>()

        fun pick(picker: SearchTypePicker<T>, otherPicker: SearchTypePicker<T>) {
            val index = picker.nextIndex(placedPageIds) ?: return
            val item = picker.items[index]
            val id = pageId(item)
            placedPageIds.add(id)
            slots.add(InterleaveSlot(item, picker.searchType, rank = index + 1, alsoInOther = otherPicker.contains(id)))
        }

        while (slots.size < listLength) {
            val lexicalHasNext = lexicalPicker.nextIndex(placedPageIds) != null
            val semanticHasNext = semanticPicker.nextIndex(placedPageIds) != null
            when {
                lexicalHasNext && semanticHasNext -> {
                    val (firstPicker, secondPicker) = if (random.nextCoin() == 0) lexicalPicker to semanticPicker else semanticPicker to lexicalPicker
                    coinFlips.add(firstPicker.searchType)
                    pick(firstPicker, secondPicker)
                    if (slots.size < listLength) {
                        pick(secondPicker, firstPicker)
                    }
                }
                lexicalHasNext -> pick(lexicalPicker, semanticPicker)
                semanticHasNext -> pick(semanticPicker, lexicalPicker)
                else -> break
            }
        }
        return InterleaveResult(slots, coinFlips, seed, MERGE_VERSION)
    }

    private class SearchTypePicker<T>(val searchType: SearchType, val items: List<T>, private val pageId: (T) -> Long) {
        private val pageIds = items.map(pageId).toSet()
        private var cursor = 0

        fun contains(id: Long): Boolean {
            return id in pageIds
        }

        fun nextIndex(placedPageIds: Set<Long>): Int? {
            while (cursor < items.size && pageId(items[cursor]) in placedPageIds) {
                cursor++
            }
            return cursor.takeIf { it < items.size }
        }
    }
}
