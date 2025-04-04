package org.wikipedia.yearinreview

import org.wikipedia.R

data class YearInReviewScreenData(
    val imageResource: Int,
    val headLineText: Int,
    val bodyText: Int
)

val getStartedData = YearInReviewScreenData(
    imageResource = R.drawable.year_in_review_block_10_resize,
    headLineText = R.string.year_in_review_get_started_headline,
    bodyText = R.string.year_in_review_get_started_bodytext
)

val readCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_block_5_resize,
    headLineText = R.string.year_in_review_read_count_headline,
    bodyText = R.string.year_in_review_read_count_bodytext
)

val editCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_bytes,
    headLineText = R.string.year_in_review_edit_count_headline,
    bodyText = R.string.year_in_review_edit_count_bodytext
)
