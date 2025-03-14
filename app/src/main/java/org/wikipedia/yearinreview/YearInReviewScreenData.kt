package org.wikipedia.yearinreview


import org.wikipedia.R

data class YearInReviewScreenData(
    val gifResource: Int,
    val headLineText: Int,
    val bodyText: Int,
    val fetchedData: Any? = null
)

val getStartedData = YearInReviewScreenData(
    gifResource = R.drawable.year_in_review_block_10_resize,
    headLineText = R.string.year_in_review_getstarted_headline,
    bodyText = R.string.year_in_review_getstarted_bodytext,
    fetchedData = null
)

val readCountData = YearInReviewScreenData(
    gifResource = R.drawable.wyir_block_5_resize,
    headLineText = R.string.year_in_review_readcount_headline,
    bodyText = R.string.year_in_review_readcount_bodytext,
    fetchedData = null
)

val editCountData = YearInReviewScreenData(
    gifResource = R.drawable.wyir_bytes,
    headLineText = R.string.year_in_review_editcount_headline,
    bodyText = R.string.year_in_review_editcount_bodytext,
    fetchedData = null
)


