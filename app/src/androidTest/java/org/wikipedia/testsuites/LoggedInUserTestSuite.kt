package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.SuggestedEditScreenTest
import org.wikipedia.tests.editing.ArticleEditingTest
import org.wikipedia.tests.explorefeed.FeedScreenSuggestedEditTest
import org.wikipedia.tests.explorefeed.MoreMenuTest

@RunWith(Suite::class)
@SuiteClasses(
    MoreMenuTest::class,
    OverflowMenuTest::class,
    ArticleEditingTest::class,
    FeedScreenSuggestedEditTest::class,
    SuggestedEditScreenTest::class
)
class LoggedInUserTestSuite
