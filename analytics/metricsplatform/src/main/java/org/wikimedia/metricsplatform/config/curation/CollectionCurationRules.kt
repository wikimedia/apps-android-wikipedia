package org.wikimedia.metricsplatform.config.curation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.function.Predicate

@Serializable
class CollectionCurationRules<T> : Predicate<Collection<T>?> {
    var contains: T? = null

    @SerialName("does_not_contain") var doesNotContain: T? = null

    @SerialName("contains_all") var containsAll: Collection<T>? = null

    @SerialName("contains_any") var containsAny: Collection<T>? = null

    override fun test(value: Collection<T>?): Boolean {
        if (value == null) return false
        return (contains == null || value.contains(contains))
                && (doesNotContain == null || !value.contains(doesNotContain))
                && (containsAll == null || value.containsAll(containsAll!!))
                && (containsAny == null || value.count { v: T? -> containsAny!!.contains(v) } > 0)
    }
}
