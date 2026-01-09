package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.DeepLinkingTest
import org.wikipedia.tests.OnboardingTest
import org.wikipedia.tests.SearchTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    OfflineTestSuite::class,
    // SuggestedEditScreenTest::class, // TODO: uncomment when login test in CI/CD is resolved
    SettingsTestSuite::class,
    ExploreFeedTestSuite::class,
    SearchTest::class,
    DeepLinkingTest::class,
    ArticlesTestSuite::class
)
class TestSuite
