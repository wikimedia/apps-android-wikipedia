package org.wikipedia.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wikipedia.createaccount.CreateAccountEncourageTest
import org.wikipedia.onboarding.InitialOnboardingActivityTest
import org.wikipedia.personalization.PersonalizationSearchLiveApiTest

@RunWith(Suite::class)
@SuiteClasses(
    InitialOnboardingActivityTest::class,
    PersonalizationSearchLiveApiTest::class,
    CreateAccountEncourageTest::class
)
class SmokeTestSuite
