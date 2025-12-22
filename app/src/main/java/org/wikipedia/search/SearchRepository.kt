package org.wikipedia.search

import org.wikipedia.Constants

interface SearchRepository<T> {
    suspend fun search(
        searchTerm: String,
        languageCode: String,
        invokeSource: Constants.InvokeSource,
        continuation: Int?,
        batchSize: Int,
        isPrefixSearch: Boolean,
        countsPerLanguageCode: MutableList<Pair<String, Int>>? = null
    ): T
}
