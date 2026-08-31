package org.wikipedia.feed.personalization

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.personalization.homepreference.HomeContentState
import org.wikipedia.feed.personalization.homepreference.HomePreferenceContent
import org.wikipedia.feed.personalization.homepreference.HomePreferenceRepository
import org.wikipedia.feed.personalization.interest.ArticlesState
import org.wikipedia.feed.personalization.interest.InterestSelectionRepository
import org.wikipedia.feed.personalization.interest.OnboardingTopic
import org.wikipedia.page.PageTitle
import org.wikipedia.topics.ArticleTopics

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PersonalizationViewModelTest {
    private val wikiSite = WikiSite(Uri.parse("https://en.wikipedia.org"), "en")
    private val interestSelectionRepository = mockk<InterestSelectionRepository>()
    private val homePreferenceRepository = mockk<HomePreferenceRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { interestSelectionRepository.wikiSite } returns wikiSite
        coEvery { homePreferenceRepository.getCommunityPreviewContent() } returns listOf(
            HomePreferenceContent(
                title = "Restoring urban rivers",
                description = null,
                imageUrl = null,
                tag = "Featured article"
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loads initial articles when nothing has been saved before`() {
        val articles = listOf(pageTitle("Apple"), pageTitle("Orange"))
        coEvery { interestSelectionRepository.getPersistedTopics() } returns emptyList()
        coEvery { interestSelectionRepository.getPersistedArticles("en") } returns emptyList()
        coEvery { interestSelectionRepository.loadInitialArticles() } returns articles

        val viewModel = viewModel()
        viewModel.onPageChanged(PersonalizationPage.INTERESTS)

        assertEquals(
            ArticlesState.Success(articles = articles, selectedArticles = emptySet()),
            viewModel.interestUiState.value.articlesState
        )
    }

    @Test
    fun `starts from previously saved topics and articles instead of the initial list`() {
        val topic = OnboardingTopic(ArticleTopics.all.first())
        val savedArticle = pageTitle("Soccer")
        val topicArticles = listOf(pageTitle("Eifel Tower"), pageTitle("Great Wall of China"))
        stubArticlesByTopic(topicArticles)
        coEvery { interestSelectionRepository.getPersistedTopics() } returns listOf(topic)
        coEvery { interestSelectionRepository.getPersistedArticles("en") } returns listOf(savedArticle)

        val viewModel = viewModel()
        viewModel.onPageChanged(PersonalizationPage.INTERESTS)

        coVerify(exactly = 0) { interestSelectionRepository.loadInitialArticles() }
        val interestUiState = viewModel.interestUiState.value
        assertEquals(2, interestUiState.totalSelectedCount)
        assertEquals(
            ArticlesState.Success(
                articles = listOf(savedArticle) + topicArticles,
                selectedArticles = setOf(savedArticle)
            ),
            interestUiState.articlesState
        )
    }

    @Test
    fun `selecting a topic saves it and replaces the article list`() {
        val topic = OnboardingTopic(ArticleTopics.all.first())
        val topicArticles = listOf(pageTitle("Building"), pageTitle("Treehouse"))
        stubArticlesByTopic(topicArticles)
        coJustRun { interestSelectionRepository.saveTopic(topic) }

        val viewModel = viewModel()
        viewModel.onTopicSelected(topic)

        coVerify { interestSelectionRepository.saveTopic(topic) }
        val interestUiState = viewModel.interestUiState.value
        assertEquals(1, interestUiState.totalSelectedCount)
        assertEquals(
            ArticlesState.Success(articles = topicArticles, selectedArticles = emptySet()),
            interestUiState.articlesState
        )
    }

    @Test
    fun `the personalized preview content is empty when the repository has nothing to preview`() {
        coEvery { homePreferenceRepository.getPersonalizedPreviewContent(any(), any()) } returns emptyList()

        val viewModel = viewModel()
        viewModel.onPageChanged(PersonalizationPage.HOME_PREFERENCE)

        assertEquals(
            HomeContentState.Empty,
            viewModel.feedPreferenceUiState.value.personalizedState
        )
    }

    @Test
    fun `the personalized preview content shows whatever the repository previews`() {
        val preview = listOf(
            HomePreferenceContent(
                title = "Sustainable architecture",
                description = null,
                imageUrl = null,
                tag = null
            )
        )
        coEvery { homePreferenceRepository.getPersonalizedPreviewContent(any(), any()) } returns preview

        val viewModel = viewModel()
        viewModel.onPageChanged(PersonalizationPage.HOME_PREFERENCE)

        assertEquals(
            HomeContentState.Success(preview),
            viewModel.feedPreferenceUiState.value.personalizedState
        )
    }

    @Test
    fun `the personalized preview content reports an error when the repository fails`() {
        coEvery {
            homePreferenceRepository.getPersonalizedPreviewContent(any(), any())
        } throws IllegalStateException("boom")

        val viewModel = viewModel()
        viewModel.onPageChanged(PersonalizationPage.HOME_PREFERENCE)

        val personalizedState = viewModel.feedPreferenceUiState.value.personalizedState
        assertEquals(
            HomeContentState.Error::class.java,
            personalizedState.javaClass
        )
    }

    private fun viewModel(): PersonalizationViewModel {
        return PersonalizationViewModel(
            interestSelectionRepository = interestSelectionRepository,
            homePreferenceRepository = homePreferenceRepository
        )
    }

    private fun stubArticlesByTopic(articles: List<PageTitle>) {
        mockkObject(InterestSelectionRepository.Companion)
        coEvery { InterestSelectionRepository.getArticlesByTopic(any(), any()) } returns articles
    }

    private fun pageTitle(title: String) = PageTitle(title, wikiSite)
}
