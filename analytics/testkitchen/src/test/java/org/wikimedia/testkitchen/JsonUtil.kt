package org.wikimedia.testkitchen

import kotlinx.serialization.json.Json

object JsonUtil {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    inline fun <reified T> decodeFromString(string: String?): T? {
        if (string.isNullOrEmpty()) {
            return null
        }
        return json.decodeFromString(string)
    }

    inline fun <reified T> encodeToString(value: T?): String? {
        if (value == null) {
            return null
        }
        return json.encodeToString(value)
    }
}
