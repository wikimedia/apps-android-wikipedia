package org.wikipedia.page

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext

/**
 * Regression test for T395597 ("text of a newly-opened article rendered under the lead
 * image of a previously-open article; pull-to-refresh reloads the wrong article").
 *
 * #6659 mitigated this by cancelling the previous [Job] in PageFragmentLoadState.pageLoad().
 * That is necessary but NOT sufficient: Kotlin cancellation is cooperative. When the
 * superseded load's network requests were already satisfied — which is exactly the case for
 * the article that was open *before* the user followed an external link to a new one, since
 * its summary is already in the HTTP / offline cache — the coroutine resumes past its
 * already-completed `await()`s without hitting a suspension point, so it never observes the
 * cancellation and runs its commit step (createPageModel) anyway. Because createPageModel
 * builds the page as `Page(title = model.title, pageProperties = thisResponse)`, the stale
 * response's lead image gets fused with the *current* article's title — the torn state users
 * see.
 *
 * The fix adds an explicit `!isActive || model.title !== title` guard before the load mutates
 * any shared state. This test models PageFragmentLoadState.pageLoad()/createPageModel()
 * faithfully (same capture-then-await-then-commit shape) and drives the two coroutines
 * through a single, manually-pumped dispatcher so the dangerous interleaving is deterministic.
 */
class PageLoadSupersessionTest {

    /** Distinct identity per article — mirrors PageTitle, which the production guard compares by
     *  reference (`!==`). Using a class avoids JVM String interning making two titles identical. */
    private class Title(val name: String) {
        override fun toString() = name
    }

    /** Stand-in for the shared PageViewModel + the two independent render channels a load
     *  mutates: the WebView (article text) and the native lead-image view. */
    private class Model {
        var title: Title? = null // PageViewModel.title (what pull-to-refresh reloads)
        var webViewText: Title? = null // what CommunicationBridge.resetHtml() loaded
        var leadImage: Title? = null // what LeadImagesHandler.loadLeadImage() showed
    }

    /** Single-threaded, manually-pumped dispatcher: nothing runs until we [drain], so we
     *  control coroutine interleaving exactly the way the Android main looper would. */
    private class ManualDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.add(block) }
        fun drain() { while (true) { (tasks.poll() ?: break).run() } }
    }

    private val dispatcher = ManualDispatcher()
    private val scope = CoroutineScope(dispatcher)
    private val model = Model()

    /**
     * Faithful model of PageFragmentLoadState.pageLoad() + createPageModel().
     * `model.title` is set synchronously first (PageFragment.loadPage does this), then the
     * coroutine captures it, loads the WebView, awaits the summary, and commits.
     */
    private fun startLoad(title: Title, summary: CompletableDeferred<Title>, guarded: Boolean): Job {
        model.title = title
        return scope.launch {
            val captured = model.title!! // `val title = model.title!!`
            model.webViewText = captured // bridge.resetHtml(title)
            val response = summary.await() // pageSummaryRequest.await()
            if (guarded && (!isActive || model.title !== captured)) {
                return@launch // <-- the fix
            }
            // createPageModel(): lead image from THIS response, title from current model.title
            model.leadImage = response // page.pageProperties.leadImageUrl (from response)
            model.title = captured // model.title = page?.title
        }
    }

    @Test
    fun `superseded load without the guard corrupts the current article (reproduces the bug)`() {
        val articleA = Title("A")
        val articleB = Title("B")

        // The user is reading A; its summary is in cache.
        val summaryA = CompletableDeferred<Title>()
        startLoad(articleA, summaryA, guarded = false)
        dispatcher.drain() // A's load runs to its await; WebView shows A
        assertSame(articleA, model.webViewText)

        // The user follows an external link to B. B supersedes A and loads (B not cached).
        val summaryB = CompletableDeferred<Title>()
        startLoad(articleB, summaryB, guarded = false)
        summaryB.complete(articleB)
        dispatcher.drain() // B commits: text + image + title all B
        assertSame(articleB, model.webViewText)
        assertSame(articleB, model.leadImage)
        assertSame(articleB, model.title)

        // NOW A's cache-served summary resolves. Its coroutine was cancelled-in-spirit, but
        // its await never suspends again, so the stale continuation runs and clobbers B.
        summaryA.complete(articleA)
        dispatcher.drain()

        // Torn state: B's text is still shown, but the lead image and the refresh target
        // have reverted to A — precisely the reported bug.
        assertSame("WebView still shows B's text", articleB, model.webViewText)
        assertSame("lead image reverted to A", articleA, model.leadImage)
        assertSame("pull-to-refresh would now reload A", articleA, model.title)
    }

    @Test
    fun `superseded load with the guard leaves the current article intact (the fix)`() {
        val articleA = Title("A")
        val articleB = Title("B")

        val summaryA = CompletableDeferred<Title>()
        startLoad(articleA, summaryA, guarded = true)
        dispatcher.drain()
        assertSame(articleA, model.webViewText)

        val summaryB = CompletableDeferred<Title>()
        startLoad(articleB, summaryB, guarded = true)
        summaryB.complete(articleB)
        dispatcher.drain()
        assertSame(articleB, model.title)

        // A's stale, cache-served summary resolves after B already committed.
        summaryA.complete(articleA)
        dispatcher.drain()

        // The guard makes the superseded load a no-op: everything stays B, consistently.
        assertSame("WebView shows B", articleB, model.webViewText)
        assertSame("lead image stays B", articleB, model.leadImage)
        assertSame("refresh reloads B", articleB, model.title)
    }
}
