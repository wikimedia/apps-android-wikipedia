package org.wikipedia.yearinreview

import org.wikipedia.R

enum class PersonalizedJobID {
    READ_COUNT,
    EDIT_COUNT,
    ONBOARDING
}

data class YearInReviewScreenData(
    val imageResource: Int,
    val headLineText: Int,
    val bodyText: Int,
    val jobId: String = ""
)

data class YearInReviewTextData(
    val headLineText: String,
    val bodyText: String
)

val getStartedData = YearInReviewScreenData(
    imageResource = R.drawable.year_in_review_block_10_resize,
    headLineText = R.string.year_in_review_get_started_headline,
    bodyText = R.string.year_in_review_get_started_bodytext,
    jobId = PersonalizedJobID.ONBOARDING.name
)

val readCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_block_5_resize,
    headLineText = R.string.year_in_review_read_count_headline,
    bodyText = R.string.year_in_review_read_count_bodytext,
    jobId = PersonalizedJobID.READ_COUNT.name
)

val editCountData = YearInReviewScreenData(
    imageResource = R.drawable.wyir_bytes,
    headLineText = R.string.year_in_review_edit_count_headline,
    bodyText = R.string.year_in_review_edit_count_bodytext,
    jobId = PersonalizedJobID.EDIT_COUNT.name
)
