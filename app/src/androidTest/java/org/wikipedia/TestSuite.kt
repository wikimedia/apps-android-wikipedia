package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.main.EditArticleTest
import org.wikipedia.main.ExploreFeedTest
import org.wikipedia.main.OnboardingTest
import org.wikipedia.main.PageTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    ExploreFeedTest::class,
    PageTest::class,
    EditArticleTest::class
)
class TestSuite
