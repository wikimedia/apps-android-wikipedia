package org.wikipedia.json

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wikipedia.util.log.L
import java.lang.Exception

object JsonUtil {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        // This "default" class discriminator is necessary for EventPlatform classes.
        // If you need a different discriminator for a different set of classes, you'll need
        // to use a separate Json object.
        classDiscriminator = "\$schema"
    }

    inline fun <reified T> decodeFromString(string: String?): T? {
        if (string == null) {
            return null
        }
        try {
            return json.decodeFromString(string)
        } catch (e: Exception) {
            L.w(e)
        }
        return null
    }

    inline fun <reified T> encodeToString(value: T): String? {
        try {
            return json.encodeToString(value)
        } catch (e: Exception) {
            L.w(e)
        }
        return null
    }
}
