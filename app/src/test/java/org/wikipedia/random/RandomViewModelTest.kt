package org.wikipedia.random

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.Constants
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.random.RandomClient
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class RandomViewModelTest {
    private val wikiSite = WikiSite(Uri.parse(""), "en")

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `articles from an earlier batch are not shown again`() {
        mockkObject(RandomClient)
        coEvery { RandomClient.getRandomPages(wikiSite, any()) } returnsMany listOf(listOf("Koala", "Wombat"), listOf("Wombat", "Dingo")).map { it.map(::pageTitle) }
        val viewModel = RandomViewModel(SavedStateHandle(mapOf(Constants.ARG_WIKISITE to wikiSite)))
        viewModel.prefetchIfNeeded(0)
        assertEquals(listOf("Koala", "Wombat", "Dingo", null), (0..3).map { viewModel.itemAt(it)?.text })
    }

    private fun pageTitle(title: String) = PageTitle(title, wikiSite, thumbUrl = "https://example.org/$title.jpg").also { it.extract = title }
}
