package org.wikipedia.feed.model

class EmptyForYouCard : Card() {
    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
