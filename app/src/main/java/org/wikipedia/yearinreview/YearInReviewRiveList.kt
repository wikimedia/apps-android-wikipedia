package org.wikipedia.yearinreview

// Property names of the List view model, shared by every list-shaped artboard (frame7, frame9, frame12, ...)
const val RIVE_LIST_VIEW_MODEL = "List"
const val RIVE_LIST_PROPERTY_HEADLINE = "headline"
const val RIVE_LIST_PROPERTY_BODY_TEXT = "bodyText"
private const val RIVE_LIST_ROW_COUNT = 3

data class RiveListRow(
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null
)

// The List view model has three numbered rows (articleTitle1, subTitle1, icon1, ...).
// Slots without a row are cleared, so the designer's sample rows never show.
fun List<RiveListRow>.toRiveListTextProperties(): Map<String, String> {
    return (1..RIVE_LIST_ROW_COUNT).flatMap { number ->
        val row = getOrNull(number - 1)
        listOf(
            "articleTitle$number" to row?.title.orEmpty(),
            "subTitle$number" to row?.subtitle.orEmpty()
        )
    }.toMap()
}

fun List<RiveListRow>.toRiveListImageUrls(): Map<String, String?> {
    return (1..RIVE_LIST_ROW_COUNT).associate { number ->
        "icon$number" to getOrNull(number - 1)?.imageUrl
    }
}
