package org.wikimedia.metrics_platform.config.curation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.function.Predicate

@Serializable
class CurationRules<T> : Predicate<T?> {
    @SerialName("equals") var isEquals: T? = null

    @SerialName("not_equals") var isNotEquals: T? = null

    @SerialName("in") var isIn: MutableCollection<T?>? = null

    @SerialName("not_in") var isNotIn: MutableCollection<T?>? = null

    override fun test(value: T?): Boolean {
        if (value == null) return false
        return (isEquals == null || isEquals == value)
                && (isNotEquals == null || isNotEquals != value)
                && (isIn == null || isIn!!.contains(value))
                && (isNotIn == null || !isNotIn!!.contains(value))
    }
}
