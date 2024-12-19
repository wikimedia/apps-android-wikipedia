package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.DeepLinkingTest
import org.wikipedia.tests.OnboardingTest
import org.wikipedia.tests.SuggestedEditScreenTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    SuggestedEditScreenTest::class,
    ExploreFeedTestSuite::class,
    SuggestedEditScreenTest::class,
    SettingsTestSuite::class,
    DeepLinkingTest::class
)
class TestSuite
