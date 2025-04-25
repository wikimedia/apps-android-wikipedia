package org.wikipedia.yearinreview

import org.wikipedia.R

data class YearInReviewScreenData(
    val imageResource: Int,
    var headLineText: Any? = null,
    var bodyText: Any? = null
)

data class YearInReviewStatistics(
    var readCount: Int = -1,
    var readCountApiTitles: List<String> = emptyList(),
    var editCount: Int = -1
)

val getStartedData = YearInReviewScreenData(
    imageResource = R.drawable.year_in_review_block_10_resize,
    headLineText = R.string.year_in_review_get_started_headline,
    bodyText = R.string.year_in_review_get_started_bodytext,
)

val readCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_block_5_resize
)

val editCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_bytes
)

val nonEnglishCollectiveReadCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_puzzle_3,
    headLineText = R.string.year_in_review_non_english_collective_readcount_headline,
    bodyText = R.string.year_in_review_non_english_collective_readcount_bodytext,
)

val nonEnglishCollectiveEditCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_puzzle_2_v5,
    headLineText = R.string.year_in_review_non_english_collective_editcount_headline,
    bodyText = R.string.year_in_review_non_english_collective_editcount_bodytext,
)
