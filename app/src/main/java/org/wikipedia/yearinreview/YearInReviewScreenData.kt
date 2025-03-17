package org.wikipedia.yearinreview

import org.wikipedia.R

data class YearInReviewScreenData(
    val gifResource: Int,
    val headLineText: Int,
    val bodyText: Int,
    val fetchedData: Any? = null
)

val readCountData = YearInReviewScreenData(
    gifResource = R.drawable.wyir_block_5_resize,
    headLineText = R.string.year_in_review_readcount_headline,
    bodyText = R.string.year_in_review_readcount_bodytext,
    fetchedData = null
)
