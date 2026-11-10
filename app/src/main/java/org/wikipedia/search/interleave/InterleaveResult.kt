package org.wikipedia.search.interleave

enum class SearchType(val logCode: Char) {
    LEXICAL('L'),
    SEMANTIC('S')
}

data class InterleaveSlot<T>(
    val item: T,
    val searchType: SearchType,
    val rank: Int,
    val alsoInOther: Boolean
)

data class InterleaveResult<T>(
    val slots: List<InterleaveSlot<T>>,
    val coinFlips: List<SearchType>,
    val seed: ULong,
    val mergeVersion: Int
) {
    val coinFlipsLog get() = coinFlips.joinToString("") { it.logCode.toString() }

    val seedLog get() = seed.toString()
}
