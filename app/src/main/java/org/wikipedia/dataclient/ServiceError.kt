package org.wikipedia.dataclient

/**
 * The API reported an error in the payload.
 */
interface ServiceError {
    val title: String
    val details: String
}
