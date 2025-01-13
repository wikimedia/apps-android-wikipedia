package org.wikipedia.testsuites

import OverflowMenuTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.articles.ArticlePageActionItemTest
import org.wikipedia.tests.articles.ArticleSectionsTest
import org.wikipedia.tests.articles.ArticleTabTest
import org.wikipedia.tests.articles.EditIconTest
import org.wikipedia.tests.articles.LeadNonLeadImageAndPreviewLinkTest
import org.wikipedia.tests.articles.MediaTest
import org.wikipedia.tests.articles.SavedArticleOnlineOfflineTest
import org.wikipedia.tests.articles.SavedArticleTest
import org.wikipedia.tests.articles.SpecialArticleTest
import org.wikipedia.tests.articles.TableOfContentsTest

@RunWith(Suite::class)
@SuiteClasses(
    ArticlePageActionItemTest::class,
    ArticleSectionsTest::class,
    ArticleTabTest::class,
    LeadNonLeadImageAndPreviewLinkTest::class,
    EditIconTest::class,
    MediaTest::class,
    OverflowMenuTest::class,
    SavedArticleTest::class,
    SpecialArticleTest::class,
    TableOfContentsTest::class,
    SavedArticleTest::class,
    SavedArticleOnlineOfflineTest::class
)
class ArticlesTestSuite
