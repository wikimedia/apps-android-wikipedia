package org.wikipedia

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.test.loggedoutuser.OnboardingTest
import org.wikipedia.test.search.SearchTest

@RunWith(Suite::class)
@SuiteClasses(
    OnboardingTest::class,
    SearchTest::class
)
class TestSuite
