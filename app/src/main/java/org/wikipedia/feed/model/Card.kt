package org.wikipedia.feed.model

abstract class Card {

    val hideKey get() = dismissHashCode().toString()

    open fun moduleKey(): String {
        return ""
    }

    open fun title(): String {
        return ""
    }

    protected open fun dismissHashCode(): Int {
        return hashCode()
    }

    override fun hashCode(): Int {
        return 31 * title().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Card) return false
        if (hideKey != other.hideKey) return false
        return true
    }
}
