package org.wikipedia.feed.announcement

import android.net.Uri
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.CardType

open class AnnouncementCard(private val announcement: Announcement) : Card() {

    val isArticlePlacement get() = Announcement.PLACEMENT_ARTICLE == announcement.placement

    override fun title(): String {
        return announcement.type
    }

    override fun extract(): String? {
        return announcement.text
    }

    override fun image(): Uri {
        return Uri.parse(announcement.imageUrl.orEmpty())
    }

    override fun type(): CardType {
        return CardType.ANNOUNCEMENT
    }

    override fun dismissHashCode(): Int {
        return announcement.id.hashCode()
    }

    fun imageHeight(): Int {
        return try {
            announcement.imageHeight.orEmpty().ifEmpty { "0" }.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }

    fun hasAction(): Boolean {
        return announcement.hasAction()
    }

    fun actionTitle(): String {
        return announcement.actionTitle()
    }

    fun actionUri(): Uri {
        return Uri.parse(announcement.actionUrl())
    }

    fun negativeText(): String? {
        return announcement.negativeText
    }

    fun hasFooterCaption(): Boolean {
        return announcement.hasFooterCaption()
    }

    fun footerCaption(): String {
        return announcement.footerCaption.orEmpty()
    }

    fun hasImage(): Boolean {
        return announcement.hasImageUrl()
    }

    fun hasBorder(): Boolean {
        return announcement.border == true
    }
}
