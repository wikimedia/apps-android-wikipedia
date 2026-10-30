package org.wikipedia.feed.model

class GamesModulePromptCard : ForYouCard() {
    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
