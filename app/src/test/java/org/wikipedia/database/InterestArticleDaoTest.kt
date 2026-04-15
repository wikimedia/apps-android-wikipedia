package org.wikipedia.database

import androidx.room.Room
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wikipedia.feed.personalization.db.dao.InterestArticleDao
import org.wikipedia.feed.personalization.db.dao.InterestTopicDao
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.page.Namespace

@RunWith(RobolectricTestRunner::class)
class InterestArticleDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var interestArticleDao: InterestArticleDao
    private lateinit var interestTopicDao: InterestTopicDao

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        interestArticleDao = db.articleInterestDao()
        interestTopicDao = db.topicInterestDao()
    }

    @Test
    fun insert_and_getAll_byLang() = runBlocking {
        val article = InterestArticle(apiTitle = "Dog", lang = "en", namespace = Namespace.MAIN, displayTitle = "Dog", description = "Animal", thumbUrl = "")
        interestArticleDao.insert(article)

        val results = interestArticleDao.getAll("en")
        assert(results.size == 1)
        assert(results[0].apiTitle == "Dog")
    }

    @Test
    fun articleInterest_foreignKey_setNull_onTopicDelete() = runBlocking {
        val topic = InterestTopic(topicId = "sports", lang = "en", topicLabel = "Sports", queryTopicId = "sports")
        interestTopicDao.insert(topic)

        val cookingTopic = InterestTopic(topicId = "Cooking", lang = "en", topicLabel = "Cooking", queryTopicId = "Cooking")
        interestTopicDao.insert(cookingTopic)

        val article1 = InterestArticle(apiTitle = "Football", lang = "en", namespace = Namespace.MAIN, displayTitle = "Football", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        interestArticleDao.insert(article1)

        val article2 = InterestArticle(apiTitle = "Basketball", lang = "en", namespace = Namespace.MAIN, displayTitle = "Basketball", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        interestArticleDao.insert(article2)

        val article3 = InterestArticle(apiTitle = "Tennis", lang = "en", namespace = Namespace.MAIN, displayTitle = "Tennis", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        interestArticleDao.insert(article3)

        val article4 = InterestArticle(apiTitle = "Cricket", lang = "en", namespace = Namespace.MAIN, displayTitle = "Cricket", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        interestArticleDao.insert(article4)

        val article5 = InterestArticle(apiTitle = "Cooking", lang = "en", namespace = Namespace.MAIN, displayTitle = "Cooking", description = "Hobby", thumbUrl = "", topicId = cookingTopic.topicId, topicLang = cookingTopic.lang)
        interestArticleDao.insert(article5)

        // Delete the topic and check that the foreign key in articles is set to null
        interestTopicDao.delete(topic)

        val results = interestArticleDao.getAll("en")
        assert(results.size == 5)

        val cookingResult = results.first { it.topicId == cookingTopic.topicId }
        assertNotNull(cookingResult.topicId)
        assertNotNull(cookingResult.topicLang)

        val sportsArticles = results.filter { it.apiTitle != "Cooking" }
        sportsArticles.forEach { article ->
            assertNull(article.topicId)
            assertNull(article.topicLang)
        }
    }

    @Test
    fun updateTopic_reassignsArticleToNewTopic() = runBlocking {
        val sportsTopic = InterestTopic(topicId = "sports", lang = "en", topicLabel = "Sports", queryTopicId = "sports")
        interestTopicDao.insert(sportsTopic)

        val technologyTopic = InterestTopic(topicId = "technology", lang = "en", topicLabel = "Technology", queryTopicId = "technology")
        interestTopicDao.insert(technologyTopic)

        val article = InterestArticle(apiTitle = "John Doe", lang = "en", namespace = Namespace.MAIN, displayTitle = "John Doe", description = "Soccer Player", thumbUrl = "", topicId = "sports", topicLang = "en")
        interestArticleDao.insert(article)

        interestArticleDao.updateTopic(newTopicId = technologyTopic.topicId, apiTitle = article.apiTitle, lang = article.lang, namespace = article.namespace)
        val results = interestArticleDao.getAll("en").first()
        assertTrue(results.topicId == technologyTopic.topicId)
    }

    @Test
    fun updateTopic_doesNotAffectOtherArticles() = runBlocking {
        val sportsTopic = InterestTopic(topicId = "sports", lang = "en", topicLabel = "Sports", queryTopicId = "sports")
        interestTopicDao.insert(sportsTopic)

        val technologyTopic = InterestTopic(topicId = "technology", lang = "en", topicLabel = "Technology", queryTopicId = "technology")
        interestTopicDao.insert(technologyTopic)

        val article1 = InterestArticle(apiTitle = "John Doe", lang = "en", namespace = Namespace.MAIN, displayTitle = "John Doe", description = "Soccer Player", thumbUrl = "", topicId = sportsTopic.topicId, topicLang = sportsTopic.lang)
        val article2 = InterestArticle(apiTitle = "Jane Doe", lang = "en", namespace = Namespace.MAIN, displayTitle = "Jane Doe", description = "Volleyball Player", thumbUrl = "", topicId = sportsTopic.topicId, topicLang = sportsTopic.lang)
        interestArticleDao.insert(article1)
        interestArticleDao.insert(article2)

        interestArticleDao.updateTopic(newTopicId = technologyTopic.topicId, apiTitle = "John Doe", lang = "en", namespace = Namespace.MAIN)

        val results = interestArticleDao.getAll("en")
        assert(results.size == 2)
        val johnDoeArticle = results.firstOrNull { it.apiTitle == "John Doe" }
        val janeDoeArticle = results.firstOrNull { it.apiTitle == "Jane Doe" }
        assertTrue(johnDoeArticle?.topicId == technologyTopic.topicId)
        assertTrue(janeDoeArticle?.topicId == "sports")
    }
}
