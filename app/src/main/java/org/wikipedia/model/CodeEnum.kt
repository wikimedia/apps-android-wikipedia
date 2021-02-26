package org.wikipedia.model

fun interface CodeEnum<T> {
    fun enumeration(code: Int): T
}
