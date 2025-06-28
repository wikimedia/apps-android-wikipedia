package org.wikimedia.metrics_platform.config.curation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.function.Predicate

@Serializable
class ComparableCurationRules<T : Comparable<T>?> : Predicate<T> {
    @SerialName("is_equals") var isEquals: T? = null

    @SerialName("not_equals") var isNotEquals: T? = null

    @SerialName("greater_than") var greaterThan: T? = null

    @SerialName("less_than") var lessThan: T? = null

    @SerialName("greater_than_or_equals") var greaterThanOrEquals: T? = null

    @SerialName("less_than_or_equals") var lessThanOrEquals: T? = null

    @SerialName("in") var isIn: MutableCollection<T?>? = null

    @SerialName("not_in") var isNotIn: MutableCollection<T?>? = null

    override fun test(value: T): Boolean {
        if (value == null) return false
        return (isEquals == null || isEquals == value)
                && (isNotEquals == null || isNotEquals != value)
                && (greaterThan == null || greaterThan!!.compareTo(value) < 0)
                && (lessThan == null || lessThan!!.compareTo(value) > 0)
                && (greaterThanOrEquals == null || greaterThanOrEquals!!.compareTo(value) <= 0)
                && (lessThanOrEquals == null || lessThanOrEquals!!.compareTo(value) >= 0)
                && (isIn == null || isIn!!.contains(value))
                && (isNotIn == null || !isNotIn!!.contains(value))
    }
}
