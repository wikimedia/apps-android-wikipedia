package org.wikipedia.dataclient.mwapi

abstract class MwResponse(val errors: List<MwServiceError>, val servedBy: String) {
    init {
        if (errors.isNotEmpty()) {
            for (error in errors) {
                // prioritize "blocked" errors over others.
                if (error.title.contains("blocked")) {
                    throw MwException(error)
                }
            }
            throw MwException(errors[0])
        }
    }
}
