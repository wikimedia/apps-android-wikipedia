package org.wikipedia.feed.personalization.feedpreference

import org.wikipedia.feed.personalization.db.dao.InterestArticleDao
import org.wikipedia.feed.personalization.db.dao.InterestTopicDao

class FeedPreferenceRepository(
    private val interestTopicDao: InterestTopicDao,
    private val interestArticleDao: InterestArticleDao
)
