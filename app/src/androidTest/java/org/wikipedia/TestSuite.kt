package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.OnboardingTest
import org.wikipedia.tests.SuggestedEditScreenTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    SuggestedEditScreenTest::class,
    ExploreFeedTestSuite::class,
)
class TestSuite
