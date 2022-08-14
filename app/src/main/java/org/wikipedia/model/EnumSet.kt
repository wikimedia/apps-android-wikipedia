package org.wikipedia.model

import java.util.*

inline fun <reified T : Enum<T>> enumSetAllOf(): EnumSet<T> {
    return EnumSet.allOf(T::class.java)
}

operator fun <T> EnumSet<T>.get(code: Int): T where T : Enum<T>, T : EnumCode {
    return find { it.code() == code } ?: throw IllegalArgumentException("code=$code")
}
