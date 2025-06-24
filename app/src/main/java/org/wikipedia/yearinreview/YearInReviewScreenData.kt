package org.wikipedia.yearinreview

data class YearInReviewScreenData(
    val animatedImageResource: Int,
    val staticImageResource: Int,
    var headLineText: Any? = null,
    var bodyText: Any? = null
)

data class YearInReviewStatistics(
    var readCount: Int = -1,
    var readCountApiTitles: List<String> = emptyList(),
    var editCount: Int = 0
)
