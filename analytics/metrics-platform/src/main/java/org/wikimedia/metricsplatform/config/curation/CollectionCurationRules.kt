package org.wikimedia.metricsplatform.config.curation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.function.Predicate

@Serializable
class CollectionCurationRules<T> : Predicate<MutableCollection<T?>?> {
    var contains: T? = null

    @SerialName("does_not_contain") var doesNotContain: T? = null

    @SerialName("contains_all") var containsAll: MutableCollection<T?>? = null

    @SerialName("contains_any") var containsAny: MutableCollection<T?>? = null

    override fun test(value: MutableCollection<T?>?): Boolean {
        if (value == null) return false
        return (contains == null || value.contains(contains))
                && (doesNotContain == null || !value.contains(doesNotContain))
                && (containsAll == null || value.containsAll(containsAll!!))
                && (containsAny == null || value.count { v: T? -> containsAny!!.contains(v) } > 0)
    }
}
