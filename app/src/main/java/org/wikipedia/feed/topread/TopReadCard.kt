package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import org.wikipedia.settings.homefeed.CommunityModuleType

@Parcelize
class TopReadCard(
    val articles: TopRead,
    val age: Int,
    val site: WikiSite
) : Card(), Parcelable {

    override fun moduleKey(): String {
        return CommunityModuleType.TOP_READ.name
    }

    override fun dismissHashCode(): Int {
        return articles.localDate.toEpochDay().toInt() + site.hashCode() + moduleKey().hashCode()
    }
}
