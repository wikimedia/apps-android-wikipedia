package org.wikipedia.json

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.wikipedia.analytics.eventplatform.Event
import org.wikipedia.analytics.eventplatform.NotificationInteractionEvent
import org.wikipedia.analytics.eventplatform.UserContributionEvent
import org.wikipedia.util.log.L
import java.lang.Exception

object JsonUtil {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        classDiscriminator = "\$schema"
        serializersModule = SerializersModule {
            polymorphic(Event::class) {
                subclass(UserContributionEvent::class)
                subclass(NotificationInteractionEvent::class)
            }
        }
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
