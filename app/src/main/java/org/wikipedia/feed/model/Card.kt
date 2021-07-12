package org.wikipedia.feed.model

import android.net.Uri

abstract class Card {

    val hideKey get() = (type().code() + dismissHashCode()).toString()

    open fun title(): String {
        return ""
    }

    open fun subtitle(): String? {
        return null
    }

    open fun image(): Uri? {
        return null
    }

    open fun extract(): String? {
        return null
    }

    abstract fun type(): CardType
    open fun onDismiss() {}
    open fun onRestore() {}

    protected open fun dismissHashCode(): Int {
        return hashCode()
    }

    override fun hashCode(): Int {
        return 31 * type().code() + title().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Card) return false
        if (hideKey != other.hideKey) return false
        return true
    }
}
