package org.wikipedia.readinglist

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.offline.db.OfflineObject
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.UriUtil
import java.io.File

data class ArticleKey(val wiki: String, val lang: String, val namespace: Namespace, val apiTitle: String)

fun ReadingListPage.articleKey() = ArticleKey(wiki.authority(), lang, namespace, apiTitle)

fun PageTitle.articleKey() = ArticleKey(wikiSite.authority(), wikiSite.languageCode, namespace(), prefixedText)

data class ArticleOfflineState(
    val key: ArticleKey,
    val requested: Boolean,
    val downloaded: Boolean,
    val pending: Boolean,
    val cleanupPending: Boolean,
    val availableBytes: Long,
    val membershipIds: Set<Long>,
    val metadata: ReadingListPage
)

/** One article's download is shared by all of its reading-list memberships. */
class ArticleOfflineStateStore(private val database: AppDatabase) {
    suspend fun snapshot(): Map<ArticleKey, ArticleOfflineState> = withContext(Dispatchers.IO) {
        aggregate(database.readingListPageDao().getActivePages(), database.offlineObjectDao().getAllObjects())
    }

    fun observe() = combine(
        database.readingListPageDao().observeActivePages(), database.offlineObjectDao().observeAllObjects()
    ) { pages, objects -> aggregate(pages, objects) }.flowOn(Dispatchers.IO)

    companion object {
        fun aggregate(pages: List<ReadingListPage>, objects: List<OfflineObject>): Map<ArticleKey, ArticleOfflineState> {
            val evidence = ArticleCacheEvidence(objects)
            return pages.filter { it.status != ReadingListPage.STATUS_QUEUE_FOR_DELETE }.groupBy { it.articleKey() }.mapValues { (key, members) ->
                val requested = members.any { it.offline }
                val completed = members.filter { it.offline && evidence.hasCompletedCopy(it) }
                val downloaded = completed.isNotEmpty()
                ArticleOfflineState(
                    key, requested, downloaded, !downloaded && members.any { it.downloadQueued },
                    !requested && members.any { evidence.hasObjectsOwnedBy(it.id) },
                    completed.maxOfOrNull { it.sizeBytes } ?: 0L,
                    members.mapTo(mutableSetOf()) { it.id }, members.minBy { it.id }
                )
            }
        }
    }
}

private val mandatoryEndpoints = listOf("page/summary/", RestService.PAGE_HTML_ENDPOINT)

fun ReadingListPage.mandatoryResponseUrls(): Set<String> = buildSet {
    val base = ServiceFactory.getRestBasePath(wiki)
    mandatoryEndpoints.forEach { endpoint ->
        val prefix = base + endpoint
        add(prefix + UriUtil.encodeURL(apiTitle))
        prefix.toHttpUrlOrNull()?.newBuilder()?.addPathSegment(apiTitle)?.build()?.let { add(it.toString()) }
    }
}

/** Checks each mandatory physical path once for this catalogue snapshot. */
class ArticleCacheEvidence(val objects: List<OfflineObject>) {
    private val objectsByOwner = mutableMapOf<Long, MutableList<OfflineObject>>().apply {
        objects.forEach { obj -> obj.usedBy.forEach { id -> getOrPut(id) { mutableListOf() }.add(obj) } }
    }
    private val presentPaths = mutableMapOf<String, Boolean>()

    fun hasObjectsOwnedBy(id: Long) = objectsByOwner[id].orEmpty().isNotEmpty()

    fun hasCompletedCopy(page: ReadingListPage): Boolean =
        page.status == ReadingListPage.STATUS_SAVED && page.sizeBytes > 0 &&
            hasMandatoryResponses(page, objectsByOwner[page.id].orEmpty())

    fun hasMandatoryResponses(page: ReadingListPage, candidates: List<OfflineObject> = objects): Boolean {
        val base = ServiceFactory.getRestBasePath(page.wiki)
        return mandatoryEndpoints.all { endpoint ->
            val prefix = base + endpoint
            candidates.any { obj ->
                obj.lang == page.lang && obj.url.startsWith(prefix) &&
                    Uri.decode(obj.url.removePrefix(prefix)) == page.apiTitle && presentPaths.getOrPut(obj.path) {
                        val metadata = File(obj.path + ".0")
                        val content = File(obj.path + ".1")
                        metadata.isFile && metadata.length() > 0 && content.isFile && content.length() > 0
                    }
            }
        }
    }
}

fun Iterable<ReadingListPage>.withOfflineState(states: Map<ArticleKey, ArticleOfflineState>): List<ReadingListPage> = map { page ->
    page.apply { articleOfflineState = states[articleKey()] }
}
