package org.wikipedia.model

import android.util.SparseArray
import androidx.core.util.valueIterator

class EnumCodeMap<T>(enumeration: Class<T>) where T : Enum<T>, T : EnumCode {
    private val map: SparseArray<T>

    init {
        map = codeToEnumMap(enumeration)
    }

    operator fun get(code: Int): T {
        return map.get(code) ?: throw IllegalArgumentException("code=$code")
    }

    private fun codeToEnumMap(enumeration: Class<T>): SparseArray<T> {
        val ret = SparseArray<T>()
        for (value in enumeration.enumConstants!!) {
            ret.put(value.code(), value)
        }
        return ret
    }

    fun size(): Int {
        return map.size()
    }

    fun valueIterator(): Iterator<T> {
        return map.valueIterator()
    }
}
