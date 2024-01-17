package org.wikipedia.extensions

import org.wikipedia.model.EnumCode
import kotlin.enums.EnumEntries

fun <E> EnumEntries<E>.getByCode(code: Int): E where E : Enum<E>, E : EnumCode {
    return getOrNull(binarySearchBy(code) { it.code() }) ?: throw IllegalArgumentException("code=$code")
}
