package org.wikipedia.feed.announcement

import org.wikipedia.feed.model.CardType

class FundraisingCard(announcement: Announcement) : AnnouncementCard(announcement) {

    override fun type(): CardType {
        return CardType.FUNDRAISING
    }
}
