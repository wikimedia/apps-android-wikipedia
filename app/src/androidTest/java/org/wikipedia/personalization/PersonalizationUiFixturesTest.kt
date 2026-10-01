package org.wikipedia.personalization

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.util.Log
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.wikipedia.base.BaseTest
import org.wikipedia.dataclient.okhttp.TestStubInterceptor
import org.wikipedia.feed.onboarding.ExploreFeedBuildingActivity
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.robots.PersonalizationRobot
import org.wikipedia.settings.Prefs
import org.wikipedia.topics.ArticleTopics

class PersonalizationUiFixturesTest : BaseTest<PersonalizationActivity>(
    activityClass = PersonalizationActivity::class.java,
    beforeActivityLaunch = {
        Prefs.shouldMatchSystemTheme = false
        TestStubInterceptor.CALLBACK = object : TestStubInterceptor.Callback {
            override fun getResponse(request: Interceptor.Chain): Response {
                val url = request.request().url.toString()
                val fixture = FIXTURE_ROUTES.entries.firstOrNull { url.contains(it.key) }?.value
                return if (fixture == null) {
                    Log.i(LOG_TAG, "no fixture -> 404 for $url")
                    respond(request, 404, "No fixture for this route", "")
                } else {
                    Log.i(LOG_TAG, "serving $fixture for $url")
                    respond(request, 200, "OK", readAsset(fixture))
                }
            }
        }
    }
) {
    private val personalizationRobot by lazy {
        PersonalizationRobot(composeTestRule, targetContext)
    }

    @Before
    fun setUpIntents() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
        TestStubInterceptor.CALLBACK = null
    }

    @Test
    fun theFirstPageOpensWithoutTouchingTheNetwork() {
        personalizationRobot
            .assertCuriosityScreenIsDisplayed()
    }

    @Test
    fun theInterestsPageShowsArticlesToChooseFrom() {
        personalizationRobot
            .assertCuriosityScreenIsDisplayed()
            .tapNext()
            .assertInterestsScreenIsDisplayed()
            .assertArticleIsDisplayed(FIRST_FIXTURE_ARTICLE)
    }

    @Test
    fun theHomePreferencePageShowsCommunityContent() {
        personalizationRobot
            .assertCuriosityScreenIsDisplayed()
            .tapNext()
            .assertInterestsScreenIsDisplayed()
            .tapNext()
            .assertHomePreferenceScreenIsDisplayed()
            .assertCommunityContentIsDisplayed(PICTURE_OF_THE_DAY_DESCRIPTION)
    }

    @Test
    fun someoneChoosesInterestsAndFinishesOnThePersonalizedFeed() {
        intending(hasComponent(ExploreFeedBuildingActivity::class.java.name))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

        personalizationRobot
            .assertCuriosityScreenIsDisplayed()
            .tapNext()
            .assertInterestsScreenIsDisplayed()
            .assertArticleIsDisplayed(FIRST_FIXTURE_ARTICLE)
            // Choosing a topic replaces the article list with that topic's articles.
            .tapTopic(ArticleTopics.all.first())
            .tapArticle(FIRST_TOPIC_ARTICLE)
            .assertSelectedCountIsDisplayed(2)
            .tapNext()
            .assertHomePreferenceScreenIsDisplayed()
            .assertPersonalizedPreferenceIsEnabled()
            .tapPersonalizedPreference()
            .tapNext()

        intended(hasComponent(ExploreFeedBuildingActivity::class.java.name))
    }

    private companion object {
        const val LOG_TAG = "PersonalizationJourney"
        const val FIRST_FIXTURE_ARTICLE = "Half sovereign"
        const val FIRST_TOPIC_ARTICLE = "List of garden types"
        const val PICTURE_OF_THE_DAY_DESCRIPTION =
            "Toco toucan (Ramphastos toco) in Mato Grosso, Brazil"

        /** URL fragment -> asset file under androidTest/assets. */
        val FIXTURE_ROUTES = mapOf(
            "generator=random" to "personalization/random_articles.json",
            "articletopic" to "personalization/topic_articles.json",
            "morelike" to "personalization/morelike_articles.json",
            // The real URL carries today's date, so match on the path instead.
            "feed/featured/" to "personalization/feed_featured.json"
        )

        fun readAsset(name: String): String {
            return InstrumentationRegistry.getInstrumentation().context.assets
                .open(name).bufferedReader().use { it.readText() }
        }

        fun respond(chain: Interceptor.Chain, code: Int, message: String, body: String): Response {
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(message)
                .body(body.toResponseBody("application/json".toMediaType()))
                .build()
        }
    }
}
