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
import org.wikipedia.onboarding.personalization.db.dao.ArticleInterestDao
import org.wikipedia.onboarding.personalization.db.dao.TopicInterestDao
import org.wikipedia.onboarding.personalization.db.entity.ArticleInterest
import org.wikipedia.onboarding.personalization.db.entity.TopicInterest
import org.wikipedia.page.Namespace

@RunWith(RobolectricTestRunner::class)
class ArticleInterestDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var articleInterestDao: ArticleInterestDao
    private lateinit var topicInterestDao: TopicInterestDao

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        articleInterestDao = db.articleInterestDao()
        topicInterestDao = db.topicInterestDao()
    }

    @Test
    fun insert_and_getAll_byLang() = runBlocking {
        val article = ArticleInterest(apiTitle = "Dog", lang = "en", namespace = Namespace.MAIN, displayTitle = "Dog", description = "Animal", thumbUrl = "")
        articleInterestDao.insert(article)

        val results = articleInterestDao.getAll("en")
        assert(results.size == 1)
        assert(results[0].apiTitle == "Dog")
    }

    @Test
    fun articleInterest_foreignKey_setNull_onTopicDelete() = runBlocking {
        val topic = TopicInterest(topicId = "sports", lang = "en", topicLabel = "Sports", queryTopicId = "sports")
        topicInterestDao.insert(topic)

        val cookingTopic = TopicInterest(topicId = "Cooking", lang = "en", topicLabel = "Cooking", queryTopicId = "Cooking")
        topicInterestDao.insert(cookingTopic)

        val article1 = ArticleInterest(apiTitle = "Football", lang = "en", namespace = Namespace.MAIN, displayTitle = "Football", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        articleInterestDao.insert(article1)

        val article2 = ArticleInterest(apiTitle = "Basketball", lang = "en", namespace = Namespace.MAIN, displayTitle = "Basketball", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        articleInterestDao.insert(article2)

        val article3 = ArticleInterest(apiTitle = "Tennis", lang = "en", namespace = Namespace.MAIN, displayTitle = "Tennis", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        articleInterestDao.insert(article3)

        val article4 = ArticleInterest(apiTitle = "Cricket", lang = "en", namespace = Namespace.MAIN, displayTitle = "Cricket", description = "Sport", thumbUrl = "", topicId = topic.topicId, topicLang = topic.lang)
        articleInterestDao.insert(article4)

        val article5 = ArticleInterest(apiTitle = "Cooking", lang = "en", namespace = Namespace.MAIN, displayTitle = "Cooking", description = "Hobby", thumbUrl = "", topicId = cookingTopic.topicId, topicLang = cookingTopic.lang)
        articleInterestDao.insert(article5)

        // Delete the topic and check that the foreign key in articles is set to null
        topicInterestDao.delete(topic)

        val results = articleInterestDao.getAll("en")
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
        val sportsTopic = TopicInterest(topicId = "sports", lang = "en", topicLabel = "Sports", queryTopicId = "sports")
        topicInterestDao.insert(sportsTopic)

        val technologyTopic = TopicInterest(topicId = "technology", lang = "en", topicLabel = "Technology", queryTopicId = "technology")
        topicInterestDao.insert(technologyTopic)

        val article = ArticleInterest(apiTitle = "John Doe", lang = "en", namespace = Namespace.MAIN, displayTitle = "John Doe", description = "Soccer Player", thumbUrl = "", topicId = "sports", topicLang = "en")
        articleInterestDao.insert(article)

        articleInterestDao.updateTopic(newTopicId = technologyTopic.topicId, apiTitle = article.apiTitle, lang = article.lang, namespace = article.namespace)
        val results = articleInterestDao.getAll("en").first()
        assertTrue(results.topicId == technologyTopic.topicId)
    }

    @Test
    fun updateTopic_doesNotAffectOtherArticles() = runBlocking {
        val sportsTopic = TopicInterest(topicId = "sports", lang = "en", topicLabel = "Sports", queryTopicId = "sports")
        topicInterestDao.insert(sportsTopic)

        val technologyTopic = TopicInterest(topicId = "technology", lang = "en", topicLabel = "Technology", queryTopicId = "technology")
        topicInterestDao.insert(technologyTopic)

        val article1 = ArticleInterest(apiTitle = "John Doe", lang = "en", namespace = Namespace.MAIN, displayTitle = "John Doe", description = "Soccer Player", thumbUrl = "", topicId = sportsTopic.topicId, topicLang = sportsTopic.lang)
        val article2 = ArticleInterest(apiTitle = "Jane Doe", lang = "en", namespace = Namespace.MAIN, displayTitle = "Jane Doe", description = "Volleyball Player", thumbUrl = "", topicId = sportsTopic.topicLabel, topicLang = sportsTopic.lang)
        articleInterestDao.insert(article1)
        articleInterestDao.insert(article2)

        articleInterestDao.updateTopic(newTopicId = technologyTopic.topicId, apiTitle = "John Doe", lang = "en", namespace = Namespace.MAIN)

        val results = articleInterestDao.getAll("en")
        assert(results.size == 2)
        val johnDoeArticle = results.firstOrNull { it.apiTitle == "John Doe" }
        val janeDoeArticle = results.firstOrNull { it.apiTitle == "Jane Doe" }
        assertTrue(johnDoeArticle?.topicId == technologyTopic.topicId)
        assertTrue(janeDoeArticle?.topicId == "sports")
    }
}
