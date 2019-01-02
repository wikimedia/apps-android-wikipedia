package org.wikipedia.model

interface CodeEnum<T> {
    fun enumeration(code: Int): T
}
