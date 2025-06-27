package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.offline.SavedArticleOnlineOfflineTest

@RunWith(Suite::class)
@SuiteClasses(
    SavedArticleOnlineOfflineTest::class
)
class OfflineTestSuite
