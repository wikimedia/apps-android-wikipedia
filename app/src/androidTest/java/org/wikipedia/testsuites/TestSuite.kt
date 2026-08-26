package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.onboarding.InitialOnboardingActivityTest

@RunWith(Suite::class)
@SuiteClasses(
    InitialOnboardingActivityTest::class
)
class TestSuite
