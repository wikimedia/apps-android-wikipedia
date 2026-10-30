package org.wikipedia.feed.model

class PlacesOfInterestLocationPromptCard : Card() {
    override fun dismissHashCode(): Int {
        return this.javaClass.simpleName.hashCode()
    }
}
