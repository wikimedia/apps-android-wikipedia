package org.wikipedia.feed.model

class EmptyCommunityCard() : Card() {
    override fun type(): CardType {
        return CardType.RANDOM // TODO: remove, since this is no longer used
    }

    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
