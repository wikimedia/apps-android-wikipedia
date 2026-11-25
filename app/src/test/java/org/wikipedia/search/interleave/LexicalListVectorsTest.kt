package org.wikipedia.search.interleave

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.json.JsonUtil
import org.wikipedia.search.StandardSearchRepository
import retrofit2.Response

/**
 * Runs the shared stage 1 lexical list vectors against the first page of [StandardSearchRepository], which is
 * where the app builds the lexical list today. The API is mocked, so each vector exercises the repository's own
 * full text threshold check and duplicate removal.
 *
 * How a vector maps to the repository:
 * - `full_text_threshold` is the `batchSize`, since the repository requests full text when it has fewer results than that.
 * - Page titles are derived one to one from page IDs, so de-duplicating by title is equivalent to de-duplicating by page ID.
 * - `full_text: null` is a full text request that fails with an HTTP error, since the repository always requests
 *   full text below the threshold.
 */
@RunWith(RobolectricTestRunner::class)
class LexicalListVectorsTest {
    private val service = mockk<Service>()

    @Before
    fun setUp() {
        mockkObject(ServiceFactory)
        every { ServiceFactory.get(any()) } returns service
        coEvery { service.prefixSearch(any(), any(), any()) } returns queryResponse(emptyList())
        coEvery { service.fullTextSearch(any(), any(), any()) } returns queryResponse(emptyList())
    }

    @After
    fun tearDown() {
        unmockkObject(ServiceFactory)
    }

    @Test
    fun testLexicalVersionMatchesVectors() {
        assertEquals(LEXICAL_VERSION, LexicalListVectors.load().lexicalVersion)
    }

    @Test
    fun testLexicalListMatchesVectors() {
        val vectors = LexicalListVectors.load().vectors
        assertTrue(vectors.isNotEmpty())

        val mismatches = vectors.mapNotNull { vector ->
            val actual = lexicalList(vector)
            if (actual == vector.expected) null else "${vector.name}: expected ${vector.expected}, but was $actual"
        }
        assertTrue("${mismatches.size} of ${vectors.size} vectors failed:\n" + mismatches.joinToString("\n"), mismatches.isEmpty())
    }

    private fun lexicalList(vector: LexicalListVectors.LexicalListVector): List<Int> {
        coEvery { service.prefixSearchResponse(any(), any(), any()) } returns Response.success(queryResponse(vector.prefix))
        val fullTextResponse: Response<MwQueryResponse> = if (vector.fullText != null) {
            Response.success(queryResponse(vector.fullText))
        } else {
            Response.error(500, "".toResponseBody())
        }
        coEvery { service.fullTextSearchResponse(any(), any(), any(), any()) } returns fullTextResponse

        return runBlocking {
            StandardSearchRepository().search(
                searchTerm = SEARCH_TERM,
                languageCode = WikipediaApp.instance.languageState.appLanguageCode,
                invokeSource = Constants.InvokeSource.SEARCH,
                continuation = null,
                batchSize = vector.fullTextThreshold,
                isPrefixSearch = true,
                countsPerLanguageCode = mutableListOf()
            )
        }.results.map { it.pageId }
    }

    private fun queryResponse(pageIds: List<Int>): MwQueryResponse {
        val pages = pageIds.mapIndexed { position, pageId ->
            """{"pageid":$pageId,"ns":0,"title":"Page $pageId","index":${position + 1}}"""
        }
        return JsonUtil.decodeFromString("""{"batchcomplete":true,"query":{"pages":[${pages.joinToString(",")}]}}""")!!
    }

    companion object {
        private const val LEXICAL_VERSION = 1

        // A single character skips the open tab, history and reading list matches, which are not inputs to the lexical list spec.
        private const val SEARCH_TERM = "a"
    }
}
