package org.wikipedia.feed.model

class DiscoverEnablePromptCard : Card() {
    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
