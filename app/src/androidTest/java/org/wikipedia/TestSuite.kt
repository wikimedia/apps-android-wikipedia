package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.tests.ExploreFeedTest
import org.wikipedia.tests.OnboardingTest
import org.wikipedia.tests.SuggestedEditScreenTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    SuggestedEditScreenTest::class,
    ExploreFeedTest::class,
)
class TestSuite
