package org.wikipedia.dataclient.mwapi

abstract class MwResponse(val errors: List<MwServiceError>, val servedBy: String) {
    init {
        if (errors.isNotEmpty()) {
            throw MwException(errors.firstOrNull { it.title.contains("blocked") } ?: errors.first())
        }
    }
}
