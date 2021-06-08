package org.wikipedia.feed.dayheader

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType
import org.wikipedia.util.DateUtil.getFeedCardDayHeaderDate

class DayHeaderCard(private val age: Int) : Card() {

    override fun title(): String {
        return getFeedCardDayHeaderDate(age)
    }

    override fun type(): CardType {
        return CardType.DAY_HEADER
    }
}
