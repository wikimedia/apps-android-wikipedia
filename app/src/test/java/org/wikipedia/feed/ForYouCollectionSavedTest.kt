package org.wikipedia.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class ForYouCollectionSavedTest {

    @Test
    fun testPlanRoundTrip() {
        val plan = listOf(
            ForYouModuleRequest.TopicInterest(age = 0, index = 0, topic = InterestTopic("topic-id")),
            ForYouModuleRequest.ArticleInterest(
                age = 0,
                index = 1,
                article = InterestArticle("Article_title", "en", Namespace.MAIN, "Article title", "description", "thumbUrl")
            ),
            ForYouModuleRequest.NewWithinInterest(age = 0, index = 0, topics = listOf(InterestTopic("other-topic-id"))),
            ForYouModuleRequest.BecauseYouRead(age = 0, index = 0),
            ForYouModuleRequest.ContinueReading(age = 0, index = 0),
            ForYouModuleRequest.Random(age = 0, index = 0)
        )
        val saved = ForYouCollectionSaved(
            dateTime = LocalDateTime.now(),
            collectionPerLanguage = mapOf("en" to ForYouLanguageCollection(plan = plan, emptySlotKeys = setOf("RANDOM-0-0")))
        )

        val decoded = JsonUtil.decodeFromString<ForYouCollectionSaved>(JsonUtil.encodeToString(saved))

        assertNotNull(decoded)
        val collection = decoded!!.collectionPerLanguage["en"]!!
        assertEquals(plan, collection.plan)
        assertEquals(setOf("RANDOM-0-0"), collection.emptySlotKeys)
    }

    @Test
    fun testSlotKeysMatchTheModulesThatFillThem() {
        val request = ForYouModuleRequest.TopicInterest(age = 1, index = 3, topic = InterestTopic("topic-id"))
        val module = ForYouModule.BasedOnInterest(age = request.age, index = request.index, cards = emptyList())
        val placeholder = ForYouModule.Placeholder(age = request.age, index = request.index, forModuleKey = request.moduleType.name)

        assertEquals("BASED_ON_INTEREST-1-3", request.slotKey)
        assertEquals(request.slotKey, module.slotKey)
        assertEquals(request.slotKey, placeholder.slotKey)
    }
}
