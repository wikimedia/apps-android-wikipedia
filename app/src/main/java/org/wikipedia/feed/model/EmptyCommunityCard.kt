package org.wikipedia.feed.model

class EmptyCommunityCard : Card() {
    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
