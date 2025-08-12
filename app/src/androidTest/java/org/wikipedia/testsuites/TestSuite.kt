package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.DeepLinkingTest
import org.wikipedia.tests.OnboardingTest
import org.wikipedia.tests.SearchTest
import org.wikipedia.tests.SuggestedEditScreenTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    OfflineTestSuite::class,
    SuggestedEditScreenTest::class,
    SettingsTestSuite::class,
    ExploreFeedTestSuite::class,
    SearchTest::class,
    SuggestedEditScreenTest::class,
    SettingsTestSuite::class,
    DeepLinkingTest::class,
    ArticlesTestSuite::class
)
class TestSuite
