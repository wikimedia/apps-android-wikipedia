package org.wikipedia.search.semantic

object SemanticSearchHelper {
    private const val DEFAULT_QUOTATION_MARK = "«"
    private val quotationMarkMap = mapOf(
        "ja" to "『",
        "ar" to "❝",
        "fr" to "«"
    )

    fun getQuotationMark(languageCode: String): String {
        return quotationMarkMap[languageCode] ?: DEFAULT_QUOTATION_MARK
    }
}
