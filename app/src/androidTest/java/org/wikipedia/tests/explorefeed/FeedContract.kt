package org.wikipedia.tests.explorefeed

import kotlinx.coroutines.runBlocking
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.featured.FeaturedArticleModuleTestTags
import org.wikipedia.feed.image.FeaturedImageModuleTestTags
import org.wikipedia.feed.topread.TopReadModuleTestTags
import java.time.LocalDate

/**
 * Independent read of the same feed endpoint the app uses, so a test knows what the Community feed
 * *should* render today. This is the diagnostic oracle: it turns live-data variance from a flake
 * source into a diagnosis — assert a module in the UI only when the backend actually served it, so
 * "the data had it but the UI didn't render it" becomes a real, named failure while a quiet day
 * (the module simply wasn't served) is not a failure at all.
 *
 * Mirrors [org.wikipedia.feed.HomeViewModel]'s fetch exactly (today, same wiki + date format).
 */
class FeedContract(private val languageCode: String) {

    /**
     * Tag → human name for every *tagged* module the backend served today. The map is the contract:
     * each entry MUST be reachable in the rendered feed. Insertion order follows feed order, so a
     * caller can scroll to each in turn monotonically.
     */
    fun servedCommunityModules(): Map<String, String> = runBlocking {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        val date = LocalDate.now()
        val content = ServiceFactory.getRest(wikiSite).getFeedFeatured(
            date.year.toString(),
            "%02d".format(date.monthValue),
            "%02d".format(date.dayOfMonth),
            languageCode
        )
        buildMap {
            content.tfa?.let { put(FeaturedArticleModuleTestTags.CARD, "Featured article") }
            content.topRead?.let { put(TopReadModuleTestTags.item(0), "Top read") }
            content.potd?.let { put(FeaturedImageModuleTestTags.CARD, "Picture of the day") }
        }
    }
}
