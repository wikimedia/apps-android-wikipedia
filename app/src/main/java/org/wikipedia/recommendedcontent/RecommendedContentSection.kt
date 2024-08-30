package org.wikipedia.recommendedcontent

import org.wikipedia.model.EnumCode

@Suppress("unused")
enum class RecommendedContentSection(val id: Int) : EnumCode {
    TOP_READ(0),
    EXPLORE(1),
    ON_THIS_DAY(2),
    IN_THE_NEWS(3),
    PLACES_NEAR_YOU(4),
    BECAUSE_YOU_READ(5),
    CONTINUE_READING(6);

    override fun code(): Int {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal
    }

    companion object {

        fun personalizedList() = listOf(EXPLORE, PLACES_NEAR_YOU)

        fun generalizedList() = listOf(TOP_READ, IN_THE_NEWS)

        fun find(id: Int): RecommendedContentSection {
            return RecommendedContentSection.entries.find { id == it.id } ?: RecommendedContentSection.entries[0]
        }
    }
}
