package org.wikipedia.feed.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.didyouknow.DidYouKnowItem
import org.wikipedia.settings.homefeed.CommunityModuleType

@Parcelize
class DidYouKnowCard(
    val items: List<DidYouKnowItem>,
    val date: String = "",
    val site: WikiSite
) : Card(), Parcelable {

    override fun moduleKey(): String {
        return CommunityModuleType.DID_YOU_KNOW.name
    }

    override fun dismissHashCode(): Int {
        return moduleKey().hashCode() + date.hashCode() + site.hashCode()
    }
}
