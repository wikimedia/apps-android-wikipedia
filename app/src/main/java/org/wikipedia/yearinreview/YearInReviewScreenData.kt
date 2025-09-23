package org.wikipedia.yearinreview

sealed class YearInReviewScreenData {
    data class StandardScreen(
        val animatedImageResource: Int,
        val staticImageResource: Int,
        val enHeadlineText: YearInReviewScreenText? = null,
        val enBodyText: YearInReviewScreenText? = null,
        val bottomButton: ButtonConfig? = null,
        val unlockIcon: UnlockIconConfig? = null
    ) : YearInReviewScreenData()

    data class HighlightsScreen(
        val highlights: List<String>,
        val headlineText: String? = null
    ) : YearInReviewScreenData()

    data class GeoScreen(
        val coordinates: Map<String, List<Int>>, // just a placeholder, @TODO: replace with actual data type
        val headlineText: String? = null,
        val bodyText: String? = null
    ) : YearInReviewScreenData()
}

data class YearInReviewScreenText(
    val defaultEnglishText: String? = null,
    val loggedInEnglishText: String? = null,
    val defaultLocalizedText: String? = null,
    val loggedInLocalizedText: String? = null
)


data class ButtonConfig(
    val text: String,
    val onClick: () -> Unit,
)

data class UnlockIconConfig(
    val iconResource: Int,
    val isUnlocked: Boolean = false,
    val onClick: (() -> Unit)? = null
)
